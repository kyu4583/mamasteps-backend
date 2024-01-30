package inhagdsc.mamasteps.map.service.tool.waypoint;

import inhagdsc.mamasteps.map.domain.LatLng;
import inhagdsc.mamasteps.map.domain.RouteRequestDto;

import java.util.ArrayList;
import java.util.List;

public class ExploratoryWaypointGenerator implements WaypointGenerator {
    private int DIVISION = 8;
    private int targetTime;
    private LatLng origin;
    private List<LatLng> intermediates;

    public ExploratoryWaypointGenerator(RouteRequestDto routeRequestDto) {
        this.targetTime = routeRequestDto.getTargetTime();
        this.origin = routeRequestDto.getOrigin();
        this.intermediates = routeRequestDto.getIntermediates();
    }

    public int getTargetTime() {
        return targetTime;
    }

    public void setTargetTime(int targetTime) {
        this.targetTime = targetTime;
    }

    public LatLng getOrigin() {
        return origin;
    }

    public void setOrigin(LatLng origin) {
        this.origin = origin;
    }

    public List<LatLng> getIntermediates() {
        return intermediates;
    }

    public void setIntermediates(List<LatLng> intermediates) {
        this.intermediates = intermediates;
    }

    public List<LatLng> getSurroundingWaypoints() {
        LatLng lastWaypoint = intermediates.get(intermediates.size() - 1);
        double smallerLat = Math.min(origin.getLatitude(), lastWaypoint.getLatitude());
        double smallerLng = Math.min(origin.getLongitude(), lastWaypoint.getLongitude());
        double largerLat = Math.max(origin.getLatitude(), lastWaypoint.getLatitude());
        double largerLng = Math.max(origin.getLongitude(), lastWaypoint.getLongitude());

        double requiredDistance = getRequiredDistance();
        double requiredDistanceInLatitude = requiredDistance / 111;
        double requiredDistanceInLongitude = (requiredDistance / 111) / Math.cos(Math.toRadians(origin.getLatitude()));

        double rightEdge = smallerLng + requiredDistanceInLongitude;
        double leftEdge = largerLng - requiredDistanceInLongitude;
        double topEdge = smallerLat + requiredDistanceInLatitude;
        double bottomEdge = largerLat - requiredDistanceInLatitude;

        double width = rightEdge - leftEdge;
        double height = topEdge - bottomEdge;
        double horizDivUnit = width / DIVISION;
        double vertDivUnit = height / DIVISION;

        LatLng[][] square = new LatLng[DIVISION + 1][DIVISION + 1];
        for (int i = 0; i <= DIVISION; i++) {
            for (int j = 0; j <= DIVISION; j++) {
                square[i][j] = new LatLng(bottomEdge + horizDivUnit * i,
                        leftEdge + vertDivUnit * j);
            }
        }

        List<LatLng> surroundingWaypoints = new ArrayList<>();
        for (int i = 0; i <= DIVISION; i++) {
            for (int j = 0; j <= DIVISION; j++) {
                double distance = getDistanceBetweenThree(origin, square[i][j], lastWaypoint);
                double longitude = square[i][j].getLongitude();
                double latitude = square[i][j].getLatitude();
                double tolerance = 2 * getDistance(longitude, latitude, longitude + horizDivUnit / 2, latitude + vertDivUnit / 2);
                if (distance < requiredDistance + tolerance && distance > requiredDistance - tolerance) {
                    surroundingWaypoints.add(square[i][j]);
                }
            }
        }

        return surroundingWaypoints;
    }

    private double getDistanceBetweenThree(LatLng first, LatLng middle, LatLng last) {
        return getDistance(
                    first.getLongitude(),
                    first.getLatitude(),
                    middle.getLongitude(),
                    middle.getLatitude()
                ) +
                getDistance(
                    middle.getLongitude(),
                    middle.getLatitude(),
                    last.getLongitude(),
                    last.getLatitude()
                );
    }

    private double getRequiredDistance() {
        List<LatLng> waypoints = LatLng.deepCopyList(intermediates);
        waypoints.add(0, origin);
        double sumOfTerms = 0;
        for (int i = 0; i < waypoints.size() - 1; i++) {
            LatLng waypoint1 = waypoints.get(i);
            LatLng waypoint2 = waypoints.get(i + 1);
            double x1 = waypoint1.getLongitude();
            double y1 = waypoint1.getLatitude();
            double x2 = waypoint2.getLongitude();
            double y2 = waypoint2.getLatitude();
            sumOfTerms += getDistance(x1, y1, x2, y2);
        }

        return (5.0 * targetTime / 60) - sumOfTerms;
    }

    private double getDistance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(
                Math.pow(111 * (y2 - y1), 2) +
                        Math.pow(111 * (x2 - x1) * Math.cos(Math.toRadians(y1)), 2)
        );
    }
}

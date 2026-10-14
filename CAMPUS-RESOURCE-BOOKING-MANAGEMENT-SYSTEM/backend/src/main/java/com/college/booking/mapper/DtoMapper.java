package com.college.booking.mapper;

import com.college.booking.dto.AuthDtos.UserResponse;
import com.college.booking.dto.BookingDtos.ApprovalView;
import com.college.booking.dto.BookingDtos.BookingView;
import com.college.booking.dto.BookingDtos.ResourceBrief;
import com.college.booking.dto.CampusDtos.BuildingSummary;
import com.college.booking.dto.CampusDtos.FloorSummary;
import com.college.booking.dto.CampusDtos.ResourceCard;
import com.college.booking.entity.Booking;
import com.college.booking.entity.BookingApproval;
import com.college.booking.entity.BookingResource;
import com.college.booking.entity.Building;
import com.college.booking.entity.Floor;
import com.college.booking.entity.Resource;
import com.college.booking.entity.ResourceFacility;
import com.college.booking.entity.User;
import com.college.booking.enums.ResourceKind;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DtoMapper {

    public UserResponse toUser(User u) {
        return new UserResponse(
                u.getId(), u.getFullName(), u.getEmail(), u.getRole(),
                u.getDepartment(), u.getPhone(), u.getNoShowCount()
        );
    }

    public BuildingSummary toBuilding(Building b, int floors, int resources, int availableNow) {
        String live = occupancyLabel(availableNow, resources);
        return new BuildingSummary(
                b.getId(), b.getName(), b.getCode(), b.getVirtueName(), b.getDescription(),
                b.getImageUrl(), b.getKind(), b.isBookable(),
                b.getMapX(), b.getMapY(), b.getMapWidth(), b.getMapHeight(),
                b.getSchematicX(), b.getSchematicY(), b.getSchematicWidth(), b.getSchematicHeight(),
                b.getDepartment(), floors, resources, availableNow, live
        );
    }

    public FloorSummary toFloor(Floor f, int classrooms, int labs, int halls, int libraries, int total, int available) {
        return new FloorSummary(
                f.getId(), f.getBuilding().getId(), f.getBuilding().getName(),
                f.getName(), f.getLevel(), f.getDescription(),
                classrooms, labs, halls, libraries, total, available
        );
    }

    public ResourceCard toResource(Resource r, List<ResourceFacility> facilities, boolean favorite, com.college.booking.enums.ResourceStatus liveStatus) {
        List<String> facilityNames = facilities.stream().map(rf -> rf.getFacility().getName()).toList();
        ResourceKind kind = r.getResourceType() != null ? r.getResourceType().getKind() : ResourceKind.CUSTOM;
        return new ResourceCard(
                r.getId(), r.getName(), r.getCode(),
                r.getResourceType() != null ? r.getResourceType().getCode() : null,
                r.getResourceType() != null ? r.getResourceType().getName() : null,
                kind,
                r.getBuilding().getId(), r.getBuilding().getName(), r.getBuilding().getCode(),
                r.getFloor().getId(), r.getFloor().getName(), r.getFloor().getLevel(),
                r.getCapacity(), r.getDepartment(), liveStatus, r.getDescription(), r.getImageUrl(),
                r.getPositionX(), r.getPositionY(), r.getWidth(), r.getHeight(), r.getRotation(),
                r.getWorkingHoursStart(), r.getWorkingHoursEnd(), null,
                r.getProjector(), r.getSmartBoard(), r.getAirConditioned(), r.getWifi(),
                r.getAudio(), r.getMicrophones(), r.getStage(), r.getComputers(),
                r.getStudySeats(), r.getReadingArea(), r.getOpeningHours(),
                r.getEquipmentNotes(), r.getSoftwareNotes(),
                facilityNames, favorite
        );
    }

    public BookingView toBooking(Booking b, List<BookingResource> resources, List<BookingApproval> approvals) {
        List<ResourceBrief> briefs = resources.stream().map(br -> {
            Resource r = br.getResource();
            return new ResourceBrief(
                    r.getId(), r.getName(), r.getCode(),
                    r.getResourceType().getName(),
                    r.getBuilding().getName(),
                    r.getFloor().getName()
            );
        }).toList();
        List<ApprovalView> av = approvals.stream().map(a -> new ApprovalView(
                a.getId(), a.getRequiredRole(), a.getStatus(), a.getComment(),
                a.getApprover() != null ? a.getApprover().getFullName() : null,
                a.getDecidedAt()
        )).toList();
        return new BookingView(
                b.getId(), b.getTitle(), b.getPurpose(), b.getBookingDate(),
                b.getStartTime(), b.getEndTime(), b.getStatus(), b.getAttendees(),
                b.getRequirements(), b.getRecurrenceType(), b.getBookingKind(),
                b.getRejectionReason(), b.getCheckInToken(),
                b.getUser().getId(), b.getUser().getFullName(), b.getUser().getEmail(), b.getUser().getRole(),
                briefs, av, b.getCreatedAt()
        );
    }

    private String occupancyLabel(int available, int total) {
        if (total <= 0) {
            return "NONE";
        }
        double used = 1.0 - (available / (double) total);
        if (used >= 0.75) {
            return "HIGH";
        }
        if (used >= 0.4) {
            return "MEDIUM";
        }
        return "LOW";
    }
}

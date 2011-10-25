package org.learningu.scheduling.logic;

import java.util.Set;

import org.learningu.scheduling.graph.Section;
import org.learningu.scheduling.graph.Program;
import org.learningu.scheduling.graph.Room;
import org.learningu.scheduling.graph.RoomProperty;
import org.learningu.scheduling.schedule.Schedule;
import org.learningu.scheduling.schedule.StartAssignment;

import com.google.common.collect.Sets;

public class RoomPropertyLogic extends ScheduleLogic {
  @Override
  public void validate(ScheduleValidator validator, Schedule schedule, StartAssignment assignment) {
    super.validate(validator, schedule, assignment);
    Program program = schedule.getProgram();
    Room room = assignment.getRoom();
    Set<RoomProperty> roomProperties = program.roomProperties(room);
    Section course = assignment.getCourse();
    Set<RoomProperty> roomRequirements = program.roomRequirements(course);
    validator.validateLocal(
        roomProperties.containsAll(roomRequirements),
        assignment,
        "courses must be assigned to rooms with all the required properties");
    Set<RoomProperty> bindingProperties = Sets.filter(roomProperties, RoomProperty.IS_BINDING);
    validator.validateLocal(
        roomRequirements.containsAll(bindingProperties),
        assignment,
        "courses must require binding properties of rooms");
  }
}
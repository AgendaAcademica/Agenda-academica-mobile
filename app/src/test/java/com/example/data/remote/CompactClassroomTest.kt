package com.example.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class CompactClassroomTest {
    @Test fun stripsCampusAndBlockFromRoom() {
        assertEquals("Sala de aula 241", compactClassroom("ICET-CAMPUS 1 BL.A-P2-SALA DE AULA 241"))
    }

    @Test fun abbreviatesLaboratory() {
        assertEquals("Lab 311", compactClassroom("Campus 1 - Laboratório 311"))
        assertEquals("Lab 311", compactClassroom("LAB. 311"))
    }

    @Test fun preservesRoomSuffixAndMultipleLocations() {
        assertEquals("Sala de aula 241A", compactClassroom("Bloco B - Sala 241A"))
        assertEquals("Sala de aula 241 / Lab 311", compactClassroom("Sala 241 / Laboratório 311"))
        assertEquals("Sala 241 e 242", compactClassroom("Sala 241 e 242"))
    }

    @Test fun simplifiesNamedComputingLaboratories() {
        assertEquals("Lab 309", compactClassroom("ICET-CAMPUS 1BL.D-P3-LAB.COMPUT. III 309"))
        assertEquals("Lab 309", compactClassroom("LAB. COMPUT. III 309"))
        assertEquals("Lab 311", compactClassroom("Laboratório de Informática II 311"))
        assertEquals("Lab 309A", compactClassroom("Lab Computação III 309A"))
        assertEquals("LAB.COMPUT. III", compactClassroom("LAB.COMPUT. III"))
    }

    @Test fun preservesLocationsWithoutNumberedRooms() {
        assertEquals("Laboratório de Informática", compactClassroom("Laboratório de Informática"))
        assertEquals("Campus 1 - Bloco A", compactClassroom("Campus 1 - Bloco A"))
        assertEquals("", compactClassroom(""))
    }

    @Test fun simplifiesApiResultBeforeReview() {
        val dto = ExtractedSyllabusDto(documentTitle = "Horário", disciplineName = "Teste",
            classroom = "CAMPUS 1 - SALA DE AULA 241")
        assertEquals("Sala de aula 241", dto.toDomain().classroom)
    }
}

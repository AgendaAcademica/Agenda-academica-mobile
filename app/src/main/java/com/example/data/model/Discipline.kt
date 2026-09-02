package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "disciplines")
data class Discipline(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val code: String = "",
    val professor: String = "",
    val classroom: String = "",
    val schedule: String = "",
    val term: String = "2026/2",
    val colorHex: String = "#2563EB",
    val syllabusNotes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

enum class ActivityType(val displayName: String, val iconName: String) {
    AULA("Aula", "School"),
    PROVA("Prova / Avaliação", "Quiz"),
    TRABALHO("Trabalho", "Assignment"),
    EXERCICIO("Exercício", "EditNote"),
    SEMINARIO("Seminário", "Groups"),
    PROJETO("Projeto", "Code"),
    LEITURA("Leitura", "MenuBook"),
    PRAZO_ENTREGA("Prazo de Entrega", "HourglassTop"),
    EVENTO("Evento Acadêmico", "Event")
}

enum class Priority(val displayName: String, val colorHex: String) {
    BAIXA("Baixa", "#10B981"),
    MEDIA("Média", "#3B82F6"),
    ALTA("Alta", "#F59E0B"),
    URGENTE("Urgente", "#EF4444")
}

enum class ActivityStatus(val displayName: String) {
    PENDENTE("Pendente"),
    EM_ANDAMENTO("Em andamento"),
    CONCLUIDA("Concluída"),
    FALTA("Falta"),
    CANCELADA("Cancelada")
}

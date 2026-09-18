package fr.stellaronstep.app.data

import fr.stellaronstep.app.core.onstep.MountStatus
import fr.stellaronstep.app.core.onstep.OnStepStatusParser
import fr.stellaronstep.app.core.onstep.OnStepTcpClient
import fr.stellaronstep.app.core.onstep.SlewDirection
import fr.stellaronstep.app.core.onstep.SlewRate

class OnStepRepository(private val client: OnStepTcpClient) {
    suspend fun readStatus(): MountStatus {
        val status = client.queryHash(":GU#")
        val ra = client.queryHash(":GR#")
        val dec = client.queryHash(":GD#")
        return OnStepStatusParser.parse(status, ra, dec)
    }

    suspend fun setSlewRate(rate: SlewRate) = client.send(rate.command)
    suspend fun startMove(direction: SlewDirection) = client.send(direction.startCommand)
    suspend fun stopMove(direction: SlewDirection) = client.send(direction.stopCommand)
    suspend fun emergencyStop() = client.send(":Q#")

    suspend fun tracking(enabled: Boolean): Boolean =
        client.queryByte(if (enabled) ":Te#" else ":Td#") == "1"

    suspend fun park(): Boolean = client.queryByte(":hP#") == "1"
    suspend fun unpark(): Boolean = client.queryByte(":hR#") == "1"
    suspend fun goHome() = client.send(":hC#")

    suspend fun goto(ra: String, dec: String): String {
        val raAccepted = client.queryByte(":Sr${ra.trim()}#") == "1"
        val decAccepted = client.queryByte(":Sd${dec.trim()}#") == "1"
        if (!raAccepted || !decAccepted) return "Coordonnées refusées"
        return when (val result = client.queryByte(":MS#")) {
            "0" -> "GOTO accepté"
            "1" -> "Cible sous l'horizon"
            "2" -> "Cible au-dessus de la limite haute"
            "3" -> "Contrôleur en veille"
            "4" -> "Monture parkée"
            "5" -> "GOTO déjà en cours"
            "6" -> "Cible hors limites"
            "7" -> "Défaut matériel"
            "8" -> "Monture déjà en mouvement"
            else -> "Réponse OnStep: $result"
        }
    }

    suspend fun startAlignment(stars: Int): Boolean = client.queryByte(":A${stars.coerceIn(1, 9)}#") == "1"
    suspend fun alignmentStatus(): String = client.queryHash(":A?#")
    suspend fun acceptAlignmentStar(): Boolean = client.queryByte(":A+#") == "1"
    suspend fun saveAlignment(): Boolean = client.queryByte(":AW#") == "1"
}

package pe.upeu.ecorecicla.backend.config

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date

class JwtService(
    private val secret: String = "EcoReciclaSecretKeyHMAC256ForJWT1234567890",
    private val issuer: String = "EcoAPI",
    private val expirationMs: Long = 3600_000L // 1 hora
) {
    private val algorithm = Algorithm.HMAC256(secret)

    fun generarToken(usuario: String, rol: String, privilegios: Set<String>): String {
        return JWT.create()
            .withIssuer(issuer)
            .withSubject(usuario)
            .withClaim("usuario", usuario)
            .withClaim("rol", rol)
            .withClaim("privilegios", privilegios.toList())
            .withExpiresAt(Date(System.currentTimeMillis() + expirationMs))
            .sign(algorithm)
    }

    val verifier = JWT.require(algorithm)
        .withIssuer(issuer)
        .build()
}

package org.apereo.cas.util.cipher;

import com.google.common.base.Throwables;
import org.apereo.cas.CipherExecutor;
import org.apereo.cas.util.EncodingUtils;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.keys.AesKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * Abstract cipher to provide common operations around signing objects.
 * @author Misagh Moayyed
 * @since 4.2
 */
public abstract class AbstractCipherExecutor<T, R> implements CipherExecutor<T, R> {
    /** Logger instance. */
    protected transient Logger logger = LoggerFactory.getLogger(this.getClass());

    private AesKey signingKey;

    /**
     * Instantiates a new cipher executor.
     *
     */
    protected AbstractCipherExecutor() {}

    /**
     * Instantiates a new cipher executor.
     *
     * @param signingSecretKey the signing key
     */
    public AbstractCipherExecutor(final String signingSecretKey) {
        setSigningKey(signingSecretKey);
    }

    public void setSigningKey(final String signingSecretKey) {
        this.signingKey = new AesKey(signingSecretKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Sign the array by first turning it into a base64 encoded string.
     *
     * @param value the value
     * @return the byte [ ]
     */
    protected byte[] sign(final byte[] value) {
        try {
            final String base64 = EncodingUtils.encodeBase64(value);
            final JsonWebSignature jws = new JsonWebSignature();
            jws.setPayload(base64);
            jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.HMAC_SHA512);
            jws.setKey(this.signingKey);
            return jws.getCompactSerialization().getBytes(StandardCharsets.UTF_8);
        } catch (final Exception e) {
            throw Throwables.propagate(e);
        }
    }

    /**
     * Verify signature.
     *
     * @param value the value
     * @return the value associated with the signature, which may have to
     * be decoded, or null.
     */
    protected byte[] verifySignature(final byte[] value) {
        try {
            final String asString = new String(value, StandardCharsets.UTF_8);
            final JsonWebSignature jws = new JsonWebSignature();
            jws.setCompactSerialization(asString);
            jws.setKey(this.signingKey);

            final boolean verified = jws.verifySignature();
            if (verified) {
                final String payload = jws.getPayload();
                logger.debug("Successfully decoded value. Result in Base64-encoding is [{}]", payload);
                return EncodingUtils.decodeBase64(payload);
            }
            return null;
        } catch (final Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public boolean isEnabled() {
        return this.signingKey != null;
    }
}

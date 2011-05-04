package me.footlights.core.crypto;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests Fingerprint using test vectors from
 * @url http://csrc.nist.gov/groups/STM/cavp/documents/shs/shabytetestvectors.zip.
 * 
 * @author Jonathan Anderson <jon@footlights.me>
 */
public class FingerprintTest
{
	@Test public void testSHA1() throws Throwable
	{
		Fingerprint.Builder builder = Fingerprint.newBuilder()
			.setAlgorithm("sha-1");

		String[][] testVectors =
		{
			{ "", "da39a3ee5e6b4b0d3255bfef95601890afd80709" },
			{ "36", "c1dfd96eea8cc2b62785275bca38ac261256e278" },
			{ "195a", "0a1c2d555bbe431ad6288af5a54f93e0449c9232" },
			{ 
				"6cb70d19c096200f9249d2dbc04299b0085eb068"
				+ "257560be3a307dbd741a3378ebfa03fcca610883"
				+ "b07f7fea563a866571822472dade8a0bec4b9820"
				+ "2d47a344312976a7bcb3964427eacb5b0525db22"
				+ "066599b81be41e5adaf157d925fac04b06eb6e01"
				+ "deb753babf33be16162b214e8db017212fafa512"
				+ "cdc8c0d0a15c10f632e8f4f47792c64d3f026004"
				+ "d173df50cf0aa7976066a79a8d78deeeec951dab"
				+ "7cc90f68d16f786671feba0b7d269d92941c4f02"
				+ "f432aa5ce2aab6194dcc6fd3ae36c8433274ef6b"
				+ "1bd0d314636be47ba38d1948343a38bf9406523a"
				+ "0b2a8cd78ed6266ee3c9b5c60620b308cc6b3a73"
				+ "c6060d5268a7d82b6a33b93a6fd6fe1de55231d12c97",

				"4a75a406f4de5f9e1132069d66717fc424376388"
			}
		};

		testVectors(builder,testVectors);
	}

	@Test public void testSHA256() throws Throwable
	{
		Fingerprint.Builder builder = Fingerprint.newBuilder()
			.setAlgorithm("sha-256");

		String[][] testVectors =
		{
			{ "", "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855" },
			{ "d3", "28969cdfa74a12c82f3bad960b0b000aca2ac329deea5c2328ebc6f2ba9802c1" },
			{ "11af", "5ca7133fa735326081558ac312c620eeca9970d1e70a4b95533d956f072d1f98" },
			{
				"6b918fb1a5ad1f9c5e5dbdf10a93a9c8f6bca89f"
				+ "37e79c9fe12a57227941b173ac79d8d440cde8c6"
				+ "4c4ebc84a4c803d198a296f3de060900cc427f58"
				+ "ca6ec373084f95dd6c7c427ecfbf781f68be572a"
				+ "88dbcbb188581ab200bfb99a3a816407e7dd6dd2"
				+ "1003554d4f7a99c93ebfce5c302ff0e11f26f83f"
				+ "e669acefb0c1bbb8b1e909bd14aa48ba3445c88b"
				+ "0e1190eef765ad898ab8ca2fe507015f1578f10d"
				+ "ce3c11a55fb9434ee6e9ad6cc0fdc4684447a9b3"
				+ "b156b908646360f24fec2d8fa69e2c93db78708f"
				+ "cd2eef743dcb9353819b8d667c48ed54cd436fb1"
				+ "476598c4a1d7028e6f2ff50751db36ab6bc32435"
				+ "152a00abd3d58d9a8770d9a3e52d5a3628ae3c9e0325",

				"46500b6ae1ab40bde097ef168b0f3199049b55545a1588792d39d594f493dca7"
			}
		};

		testVectors(builder,testVectors);
	}


	private void testVectors(Fingerprint.Builder builder, String[][] testVectors)
			throws DecoderException
	{
		for (String[] vectors : testVectors)
		{
			byte[] input = Hex.decodeHex(vectors[0].toCharArray());
			byte[] expected = Hex.decodeHex(vectors[1].toCharArray());

			Fingerprint fingerprint = builder.setContent(input).build();
			byte[] output = new byte[fingerprint.getBytes().remaining()];
			fingerprint.getBytes().get(output);

			assertTrue(fingerprint.matches(expected));
			assertArrayEquals(expected, output);
			assertEquals(vectors[1], fingerprint.hex());
		}
	}
}

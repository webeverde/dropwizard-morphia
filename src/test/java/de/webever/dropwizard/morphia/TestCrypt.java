package de.webever.dropwizard.morphia;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

import de.webever.dropwizard.morphia.crypt.Cryptor;
import de.webever.dropwizard.morphia.model.Model;

public class TestCrypt {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {

    }

    @Test
    public void testCrypt() throws IOException {

	Cryptor.initCrypt("1234567890123456", Model.class.getPackage().getName(), true);
	String test = "Hellö@Würld.com!";
	String enc = Cryptor.encrypt(test);
	String dec = Cryptor.decrypt(enc);
	assertThat("Strings should be equal", dec, equalTo(test));
    }

}

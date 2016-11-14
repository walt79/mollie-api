package nl.stil4m.mollie.concepts;

import static nl.stil4m.mollie.TestUtil.TEST_TIMEOUT;
import static nl.stil4m.mollie.TestUtil.VALID_API_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import nl.stil4m.mollie.Client;
import nl.stil4m.mollie.ClientBuilder;
import nl.stil4m.mollie.ResponseOrError;
import nl.stil4m.mollie.domain.CreatePayment;
import nl.stil4m.mollie.domain.CreateRefund;
import nl.stil4m.mollie.domain.Payment;
import nl.stil4m.mollie.domain.Page;
import nl.stil4m.mollie.domain.Refund;

public class RefundsIntegrationTest {

    private Client client;
    private Payments payments;

    @Before
    public void before() throws InterruptedException {
        Thread.sleep(TEST_TIMEOUT);
        client = new ClientBuilder().withApiKey(VALID_API_KEY).build();
        payments = client.payments();
    }

    @Test
    public void testGetRefunds() throws IOException, URISyntaxException, InterruptedException {
        ResponseOrError<Payment> payment = payments.create(new CreatePayment(Optional.empty(), 1.00, "Some description", "http://example.com", Optional.empty(), null));
        String id = payment.getData().getId();
        Refunds refunds = client.refunds(id);
        
        ResponseOrError<Page<Refund>> all = refunds.all(Optional.empty(), Optional.empty());
        
        assertThat(all.getSuccess(), is(true));
        Page<Refund> refundPage = all.getData();
        assertThat(refundPage.getCount(), is(0));
        assertThat(refundPage.getData(), is(notNullValue()));
        assertThat(refundPage.getLinks(), is(notNullValue()));
    }

    @Test
    public void testListRefundsForExistingPayment() throws IOException, URISyntaxException, InterruptedException {
        Payment payment = payments.create(new CreatePayment(Optional.empty(), 1.00, "Some description", "http://example.com", Optional.empty(), null)).getData();
        Refunds refunds = client.refunds(payment.getId());
        
        ResponseOrError<Page<Refund>> all = refunds.all(Optional.empty(), Optional.empty());

        assertThat(all.getSuccess(), is(true));
        Page<Refund> data = all.getData();
        assertThat(data.getData().size(), is(0));
    }


    @Test
    public void testCancelNonExistingRefund() throws IOException {
        Map<String, String> errorData = new HashMap<>();
        errorData.put("type", "request");
        errorData.put("message", "The refund id is invalid");
        ResponseOrError<Payment> payment = payments.create(new CreatePayment(Optional.empty(), 1.00, "Some description", "http://example.com", Optional.empty(), null));
        assertThat(payment.getSuccess(), is(true));
        Refunds refunds = client.refunds(payment.getData().getId());
        
        ResponseOrError<Void> cancel = refunds.delete("foo_bar");
        
        assertThat(cancel.getSuccess(), is(false));
        assertThat(cancel.getError().get("error"), is(errorData));
    }

    @Test
    public void testGetNonExistingRefund() throws IOException {
        Map<String, String> errorData = new HashMap<>();
        errorData.put("type", "request");
        errorData.put("message", "The refund id is invalid");
        ResponseOrError<Payment> payment = payments.create(new CreatePayment(Optional.empty(), 1.00, "Some description", "http://example.com", Optional.empty(), null));
        Refunds refunds = client.refunds(payment.getData().getId());

        ResponseOrError<Refund> get = refunds.get("foo_bar");
        
        assertThat(get.getSuccess(), is(false));
        assertThat(get.getError().get("error"), is(errorData));
    }

    @Test
    public void testCreateRefund() throws IOException, URISyntaxException {
        Map<String, String> errorData = new HashMap<>();
        errorData.put("type", "request");
        errorData.put("message", "The payment is already refunded or has not been paid for yet");
        ResponseOrError<Payment> payment = payments.create(new CreatePayment(Optional.empty(), 1.00, "Some description", "http://example.com", Optional.empty(), null));
        Refunds refunds = client.refunds(payment.getData().getId());
        
        CreateRefund createRefund = new CreateRefund(1.00);
        ResponseOrError<Refund> create = refunds.create(createRefund);
        
        assertThat(create.getSuccess(), is(false));
        assertThat(create.getError().get("error"), is(errorData));
    }
}

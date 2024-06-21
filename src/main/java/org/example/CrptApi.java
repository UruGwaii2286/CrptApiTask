package org.example;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

public class CrptApi {
	private final int requestLimit;
	private final TimeUnit timeUnit;
	private final AtomicInteger requestCount = new AtomicInteger(0);
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private final ReentrantLock lock = new ReentrantLock();
	private final OkHttpClient httpClient = new OkHttpClient();
	private final ObjectMapper objectMapper = new ObjectMapper();

	public CrptApi(TimeUnit timeUnit, int requestLimit) {
		this.timeUnit = timeUnit;
		this.requestLimit = requestLimit;
		scheduler.scheduleAtFixedRate(() -> requestCount.set(0), 0, 1, timeUnit);
	}

	public void createDocument(Document document, String signature) throws InterruptedException {
		lock.lock();
		try {
			while (requestCount.get() >= requestLimit) {
				lock.unlock();
				Thread.sleep(100);
				lock.lock();
			}
			requestCount.incrementAndGet();
		} finally {
			lock.unlock();
		}

		try {
			String json = objectMapper.writeValueAsString(document);
			RequestBody body = RequestBody.create(json, MediaType.get("application/json"));
			Request request = new Request.Builder()
					.url("https://httpbin.org/post")
					.post(body)
					.addHeader("Signature", signature)
					.build();

			try (Response response = httpClient.newCall(request).execute()) {
				if (!response.isSuccessful()) throw new RuntimeException("Unexpected code " + response);
				System.out.println(response.body().string());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static class Document {
		public Description description;
		public String doc_id;
		public String doc_status;
		public String doc_type;
		public boolean importRequest;
		public String owner_inn;
		public String participant_inn;
		public String producer_inn;
		public String production_date;
		public String production_type;
		public Product[] products;
		public String reg_date;
		public String reg_number;

		public static class Description {
			public String participantInn;
		}

		public static class Product {
			public String certificate_document;
			public String certificate_document_date;
			public String certificate_document_number;
			public String owner_inn;
			public String producer_inn;
			public String production_date;
			public String tnved_code;
			public String uit_code;
			public String uitu_code;
		}
	}

	public static void main(String[] args) {
		CrptApi crptApi = new CrptApi(TimeUnit.MINUTES, 5);

		Document document = new Document();
		document.doc_id = "12345";
		document.doc_status = "DRAFT";
		document.doc_type = "LP_INTRODUCE_GOODS";
		document.importRequest = true;
		document.owner_inn = "1234567890";
		document.participant_inn = "0987654321";
		document.producer_inn = "1122334455";
		document.production_date = "2023-06-20";
		document.production_type = "TYPE";
		document.reg_date = "2023-06-21";
		document.reg_number = "67890";

		Document.Description description = new Document.Description();
		description.participantInn = "0987654321";
		document.description = description;

		Document.Product product = new Document.Product();
		product.certificate_document = "CERT_DOC";
		product.certificate_document_date = "2023-06-20";
		product.certificate_document_number = "123456";
		product.owner_inn = "1234567890";
		product.producer_inn = "1122334455";
		product.production_date = "2023-06-20";
		product.tnved_code = "12345678";
		product.uit_code = "UIT123456";
		product.uitu_code = "UITU123456";

		document.products = new Document.Product[]{product};

		String signature = "";

		try {
			for (int i = 0; i < 10; i++) {
				crptApi.createDocument(document, signature);
				System.out.println("Request " + (i + 1) + " sent.");
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}

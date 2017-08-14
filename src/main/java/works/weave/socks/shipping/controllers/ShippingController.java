package works.weave.socks.shipping.controllers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.ChannelCallback;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.rabbitmq.client.Channel;

import works.weave.socks.shipping.entities.HealthCheck;
import works.weave.socks.shipping.entities.Shipment;

@RestController
public class ShippingController {

	private static int sleep = 0;

	@Autowired
	RabbitTemplate rabbitTemplate;

	@RequestMapping(value = "/shipping", method = RequestMethod.GET)
	public String getShipping() {
		try {
			Thread.sleep(this.getSleep());
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "GET ALL Shipping Resource.";
	}

	@RequestMapping(value = "/shipping/{id}", method = RequestMethod.GET)
	public String getShippingById(@PathVariable String id) {
		try {
			Thread.sleep(this.getSleep());
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "GET Shipping Resource with id: " + id;
	}

	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = "/shipping", method = RequestMethod.POST)
	public @ResponseBody Shipment postShipping(@RequestBody Shipment shipment) {
		try {
			Thread.sleep(this.getSleep());
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Adding shipment to queue...");
		try {
			rabbitTemplate.convertAndSend("shipping-task", shipment);
		} catch (Exception e) {
			System.out.println("Unable to add to queue (the queue is probably down). Accepting anyway. Don't do this "
					+ "for real!");
		}
		return shipment;
	}

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(method = RequestMethod.GET, path = "/health")
	public @ResponseBody Map<String, List<HealthCheck>> getHealth() {
		try {
			Thread.sleep(this.getSleep());
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Map<String, List<HealthCheck>> map = new HashMap<String, List<HealthCheck>>();
		List<HealthCheck> healthChecks = new ArrayList<HealthCheck>();
		Date dateNow = Calendar.getInstance().getTime();

		HealthCheck rabbitmq = new HealthCheck("shipping-rabbitmq", "OK", dateNow);
		HealthCheck app = new HealthCheck("shipping", "OK", dateNow);

		try {
			this.rabbitTemplate.execute(new ChannelCallback<String>() {
				@Override
				public String doInRabbit(Channel channel) throws Exception {
					Map<String, Object> serverProperties = channel.getConnection().getServerProperties();
					return serverProperties.get("version").toString();
				}
			});
		} catch (AmqpException e) {
			rabbitmq.setStatus("err");
		}

		healthChecks.add(rabbitmq);
		healthChecks.add(app);

		map.put("health", healthChecks);
		return map;
	}

	@RequestMapping(value = "/sleep/{sleep}", method = RequestMethod.GET)
	public int setSleep(@PathVariable int sleep) {
		int oldSleep = this.sleep;
		this.sleep = sleep;
		return oldSleep;
	}

	private int getSleep() {
		URLConnection conn;
		try {
			conn = new URL("http://tobias-angerstein.de/sleep").openConnection();
			conn.connect();

			try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
				String inputLine = in.readLine();
				if(inputLine.equals("Default")) {
					return sleep;
				}
				return Integer.parseInt(inputLine);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
}

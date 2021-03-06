/*
 * (C) Copyright 2017-2020 OpenVidu (https://openvidu.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.openvidu.java.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * See {@link io.openvidu.java.client.Session#getActiveConnections()}
 */
public class Connection {

	private String connectionId;
	private long createdAt;
	private String location;
	private String platform;
	private String clientData;
	private Token token;

	protected Map<String, Publisher> publishers = new ConcurrentHashMap<>();
	protected List<String> subscribers = new ArrayList<>();

	protected Connection(JsonObject json) {
		JsonArray jsonArrayPublishers = json.get("publishers").getAsJsonArray();
		jsonArrayPublishers.forEach(publisher -> {
			JsonObject pubJson = publisher.getAsJsonObject();
			JsonObject mediaOptions = pubJson.get("mediaOptions").getAsJsonObject();
			Publisher pub = new Publisher(pubJson.get("streamId").getAsString(), pubJson.get("createdAt").getAsLong(),
					mediaOptions.get("hasAudio").getAsBoolean(), mediaOptions.get("hasVideo").getAsBoolean(),
					mediaOptions.get("audioActive"), mediaOptions.get("videoActive"), mediaOptions.get("frameRate"),
					mediaOptions.get("typeOfVideo"), mediaOptions.get("videoDimensions"));
			this.publishers.put(pub.getStreamId(), pub);
		});

		JsonArray jsonArraySubscribers = json.get("subscribers").getAsJsonArray();
		jsonArraySubscribers.forEach(subscriber -> {
			this.subscribers.add((subscriber.getAsJsonObject()).get("streamId").getAsString());
		});

		this.connectionId = json.get("connectionId").getAsString();
		this.createdAt = json.get("createdAt").getAsLong();

		this.location = json.get("location").getAsString();
		this.platform = json.get("platform").getAsString();
		this.clientData = json.get("clientData").getAsString();

		String token = json.has("token") ? json.get("token").getAsString() : null;
		OpenViduRole role = OpenViduRole.valueOf(json.get("role").getAsString());
		String data = json.get("serverData").getAsString();
		Boolean record = json.get("record").getAsBoolean();

		TokenOptions tokenOptions = new TokenOptions(role, data, record, null);
		this.token = new Token(token, this.connectionId, tokenOptions);
	}

	/**
	 * Returns the identifier of the connection. You can call methods
	 * {@link io.openvidu.java.client.Session#forceDisconnect(String)} or
	 * {@link io.openvidu.java.client.Session#updateConnection(String, TokenOptions)}
	 * passing this property as parameter
	 */
	public String getConnectionId() {
		return connectionId;
	}

	/**
	 * Timestamp when this connection was established, in UTC milliseconds (ms since
	 * Jan 1, 1970, 00:00:00 UTC)
	 */
	public long createdAt() {
		return this.createdAt;
	}

	/**
	 * Returns the role of the connection
	 */
	public OpenViduRole getRole() {
		return this.token.getRole();
	}

	/**
	 * Returns the data associated to the connection on the server-side. This value
	 * is set with {@link io.openvidu.java.client.TokenOptions.Builder#data(String)}
	 * when calling {@link io.openvidu.java.client.Session#generateToken()}
	 */
	public String getServerData() {
		return this.token.getData();
	}

	/**
	 * Whether the streams published by this Connection will be recorded or not.
	 * This only affects <a href=
	 * "https://docs.openvidu.io/en/stable/advanced-features/recording#selecting-streams-to-be-recorded"
	 * target="_blank">INDIVIDUAL recording</a>.
	 */
	public boolean record() {
		return this.token.record();
	}

	/**
	 * Returns the token string associated to the connection
	 */
	public String getToken() {
		return this.token.getToken();
	}

	/**
	 * <a href="https://docs.openvidu.io/en/stable/openvidu-pro/" target="_blank"
	 * style="display: inline-block; background-color: rgb(0, 136, 170); color:
	 * white; font-weight: bold; padding: 0px 5px; margin-right: 5px; border-radius:
	 * 3px; font-size: 13px; line-height:21px; font-family: Montserrat,
	 * sans-serif">PRO</a>
	 * 
	 * Returns the geo location of the connection, with the following format:
	 * <code>"CITY, COUNTRY"</code> (<code>"unknown"</code> if it wasn't possible to
	 * locate it)
	 */
	public String getLocation() {
		return location;
	}

	/**
	 * Returns a complete description of the platform used by the participant to
	 * connect to the session
	 */
	public String getPlatform() {
		return platform;
	}

	/**
	 * Returns the data associated to the connection on the client-side. This value
	 * is set with second parameter of method <a href=
	 * "https://docs.openvidu.io/en/stable/api/openvidu-browser/classes/session.html#connect"
	 * target ="_blank">Session.connect</a> in OpenVidu Browser
	 */
	public String getClientData() {
		return clientData;
	}

	/**
	 * Returns the list of Publisher objects this particular Connection is
	 * publishing to the Session (each Publisher object has one Stream, uniquely
	 * identified by its <code>streamId</code>). You can call
	 * {@link io.openvidu.java.client.Session#forceUnpublish(Publisher)} passing any
	 * of this values as parameter
	 */
	public List<Publisher> getPublishers() {
		return new ArrayList<>(this.publishers.values());
	}

	/**
	 * Returns the list of streams (their <code>streamId</code> properties) this
	 * particular Connection is subscribed to. Each one always corresponds to one
	 * Publisher of some other Connection: each string of the returned list must be
	 * equal to the returned value of some
	 * {@link io.openvidu.java.client.Publisher#getStreamId()}
	 */
	public List<String> getSubscribers() {
		return this.subscribers;
	}

	/**
	 * For now only properties data, role and record can be updated
	 */
	protected void overrideTokenOptions(TokenOptions tokenOptions) {
		TokenOptions.Builder modifiableTokenOptions = new TokenOptions.Builder().role(tokenOptions.getRole())
				.record(tokenOptions.record());
		modifiableTokenOptions.data(this.token.getData());
		this.token.overrideTokenOptions(modifiableTokenOptions.build());
	}

	protected void setSubscribers(List<String> subscribers) {
		this.subscribers = subscribers;
	}

}

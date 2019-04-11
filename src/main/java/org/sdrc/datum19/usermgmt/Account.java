package org.sdrc.datum19.usermgmt;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.springframework.data.annotation.Id;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;


@Entity
@Data
@Table(uniqueConstraints = @UniqueConstraint(columnNames="user_name"))
public class Account implements Serializable {

	private static final long serialVersionUID = 1416515536173441248L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(name = "user_name")
	private String userName;

	@JsonIgnore
	private String password;

	@Column(name = "enabled", columnDefinition = "boolean DEFAULT true")
	private boolean enabled = true;

	@Column(name = "credentialexpired", columnDefinition = "boolean DEFAULT false")
	private boolean credentialexpired = false;

	@Column(name = "expired", columnDefinition = "boolean DEFAULT false")
	private boolean expired = false;

	@Column(name = "locked", columnDefinition = "boolean DEFAULT false")
	private boolean locked = false;

	private String email;

	@JsonIgnore
	@OneToMany(mappedBy = "account", fetch = FetchType.LAZY)
	private List<AccountDesignationMapping> accountDesignationMapping;

	/**
	 * for forgot password
	 */
	@Column
	@JsonIgnore
	private String otp;

	@JsonIgnore
	@Column(name = "invalid_attempts", columnDefinition = "smallint default '0'")
	private short invalidAttempts;

	@JsonIgnore
	@Column(name = "last_otp_generated_time")
	private Date otpGeneratedDateTime;

	// constructor
	public Account() {
		super();
	}

	public Account(Integer id) {
		super();
		this.id = id;
	}
}
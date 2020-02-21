package com.upgrad.FoodOrderingApp.api.controller;

import com.upgrad.FoodOrderingApp.api.model.LoginResponse;
import com.upgrad.FoodOrderingApp.api.model.SignupCustomerRequest;
import com.upgrad.FoodOrderingApp.api.model.SignupCustomerResponse;
import com.upgrad.FoodOrderingApp.service.businness.AuthenticationService;
import com.upgrad.FoodOrderingApp.service.businness.CustomerService;
import com.upgrad.FoodOrderingApp.service.entity.CustomerAuthEntity;
import com.upgrad.FoodOrderingApp.service.entity.CustomerEntity;
import com.upgrad.FoodOrderingApp.service.exception.AuthenticationFailedException;
import com.upgrad.FoodOrderingApp.service.exception.SignUpRestrictedException;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@CrossOrigin
@RequestMapping("/")
public class CustomerController {

  @Autowired
  private CustomerService customerService;

  /**
   * Handler to signup for any prospective customer to get registered.
   *
   * @param signupCustomerRequest SignupCustomerRequest
   * @return SingupCustomerResponse
   * @throws SignUpRestrictedException SignUpRestrictedException
   */
  @RequestMapping(method = RequestMethod.POST, path = "/customer/signup",
      consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
      produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
  public ResponseEntity<SignupCustomerResponse> customerSignup(final SignupCustomerRequest signupCustomerRequest)
      throws SignUpRestrictedException {

    //Fetch details from signupCustomerRequest and set in CustomerEntity instance
    if(signupCustomerRequest==null) throw new SignUpRestrictedException("SGR-005", "Except last name all fields should be filled");
    final CustomerEntity customerEntity = new CustomerEntity();
    customerEntity.setUuid(UUID.randomUUID().toString());
    customerEntity.setFirstName(signupCustomerRequest.getFirstName());
    customerEntity.setLastName(signupCustomerRequest.getLastName());
    customerEntity.setEmail(signupCustomerRequest.getEmailAddress());
    customerEntity.setPassword(signupCustomerRequest.getPassword());
    customerEntity.setContactnumber(signupCustomerRequest.getContactNumber());
    customerEntity.setSalt("1234abc"); // will get overwritten in service class

    //Invoke business Service to signup & return SignupCustomerResponse
    final CustomerEntity createdCustomerEntity = customerService.saveCustomer(customerEntity);
    SignupCustomerResponse customerResponse = new SignupCustomerResponse().id(createdCustomerEntity.getUuid())
        .status("CUSTOMER SUCCESSFULLY REGISTERED");
    return new ResponseEntity<SignupCustomerResponse>(customerResponse, HttpStatus.CREATED);
  }


  /**
   * Handler to login
   *
   * @param authorization access token
   * @return SigninResponse
   * @throws AuthenticationFailedException AuthenticationFailedException
   */
  @RequestMapping(method = RequestMethod.POST, path = "/customer/login",
      produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
  public ResponseEntity<LoginResponse> login(
      @RequestHeader("authorization") final String authorization)
      throws AuthenticationFailedException {

    //Split authorization header to get username and password
    byte[] decode = null;
    String[] tokens = authorization.split("Basic ");

    try {
      decode = Base64.getDecoder().decode(tokens[1]);
    } catch (IllegalArgumentException ile) {
      throw new AuthenticationFailedException("ATH-003",
          "Incorrect format of decoded customer name and password");
    } catch (IndexOutOfBoundsException ie) {
      throw new AuthenticationFailedException("ATH-003",
          "Incorrect format of decoded customer name and password");
    }
    String decodedText = new String(decode);
    String[] decodedArray = decodedText.split(":");
    String contactNumber, password;
    try{
       contactNumber = decodedArray[0];
       password = decodedArray[1];

    }catch(Exception e){
      throw new AuthenticationFailedException("ATH-003",
          "Incorrect format of decoded customer name and password");

    }

    //Invoke Authentication Service
    CustomerAuthEntity customerAuthEntity = customerService
        .authenticate(contactNumber, password);

    //Get Customer details
    CustomerEntity customer = customerAuthEntity.getCustomer();

    //Fill LoginResponse and return
    LoginResponse loginResponse = new LoginResponse().id(customer.getUuid())
        .firstName(customer.getFirstName()).lastName(customer.getLastName())
        .contactNumber(customer.getContactnumber()).emailAddress(customer.getEmail())
        .message("LOGGED IN SUCCESSFULLY");
    HttpHeaders headers = new HttpHeaders();
    headers.add("access-token", customerAuthEntity.getAccessToken());
    return new ResponseEntity<LoginResponse>(loginResponse, headers, HttpStatus.OK);
  }

}

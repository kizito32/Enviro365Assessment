package com.enviro.assessment.junior.kizitokayizzi.security;

import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;

/** Spring Security user that also carries the investor id, so controllers never trust an id from the client. */
public class InvestorUserDetails extends User {

    private final Long investorId;

    public InvestorUserDetails(String username, String passwordHash, Long investorId) {
        super(username, passwordHash, AuthorityUtils.createAuthorityList("ROLE_INVESTOR"));
        this.investorId = investorId;
    }

    public Long getInvestorId() {
        return investorId;
    }
}

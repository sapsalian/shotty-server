package com.shotty.shotty.domain.bid.domain;

import com.shotty.shotty.domain.apply.domain.Apply;
import com.shotty.shotty.domain.payment.domain.Payment;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "bids")
public class Bid {
    @Id
    @GeneratedValue
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apply_id")
    private Apply apply;

    private String shortsId;

    private Boolean accepted = false;

    @OneToOne(mappedBy = "bid",cascade = CascadeType.REMOVE)
    private Payment payment;

    //연관관계 메서드
    private void setApply(Apply apply) {
        this.apply = apply;
    }

    public Bid(Apply apply) {
        setApply(apply);
        shortsId = null;
    }

    public void setShortsId(String shortsId) {
        this.shortsId = shortsId;
    }

    public void accept() {
        this.accepted = true;
    }
}

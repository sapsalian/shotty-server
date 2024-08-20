package com.shotty.shotty.domain.balance.application;

import com.shotty.shotty.domain.balance.dto.BalanceResDto;
import com.shotty.shotty.domain.balance.dto.ChangeBalanceDto;
import com.shotty.shotty.domain.user.dao.UserRepository;
import com.shotty.shotty.domain.user.domain.User;
import com.shotty.shotty.domain.user.exception.custom_exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BalanceService {
    private final UserRepository userRepository;

    public BalanceResDto getBalanceByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("존재하지 않는 사용자입니다."));

        return BalanceResDto.from(user);
    }

    public BalanceResDto deposit(Long userId, ChangeBalanceDto changeBalanceDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("존재하지 않는 사용자입니다."));


        user.deposit(changeBalanceDto.amount());
        user = userRepository.save(user);

        return BalanceResDto.from(user);
    }
}

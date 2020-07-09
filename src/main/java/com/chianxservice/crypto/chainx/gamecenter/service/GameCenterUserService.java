package com.chianxservice.crypto.chainx.gamecenter.service;

import com.chainxservice.chainx.UserService;
import com.chainxservice.crypto.gamecentral.repository.ArticleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.dubbo.config.annotation.Reference;

@Service
public class GameCenterUserService {

	@Reference
	UserService userService;

	@Autowired
	ArticleRepository articleRepository;
}

package com.MS.history.service;

import com.MS.history.client.UserFeign;
import com.MS.history.model.*;
import com.MS.history.model.DTO.CheckoutHistory;
import com.MS.history.model.DTO.HistoryDTO;
import com.MS.history.model.DTO.PaymentDTO;
import com.MS.history.model.DTO.UserDTO;
import com.MS.history.repository.*;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Service
public class HistoryService {

    ModelMapper modelMapper = new ModelMapper();
    @Autowired
    UserFeign userFeign;
    @Autowired
    SequenceGeneratorService sequenceGeneratorService;
    @Autowired
    HistoryRepository historyRepository;
    @Autowired
    PaymentRepository paymentRepository;
    @Autowired
    ProductRepository productRepository;
    @Autowired
    PurchaseRepository purchaseRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    MongoTemplate mongoTemplate;


    public HistoryDTO getById(Long user_id){
        User user = userRepository.findByUser_id(user_id);
        Long history_id = user.getHistory_id();
        History history = historyRepository.findByHistory_id(history_id);
        history.setUser(userRepository.findByHistory_id(history_id));
        HistoryDTO historyDTO = modelMapper.map(history,HistoryDTO.class);
        return historyDTO;
    }

    public String dateFormatter(String oldDate){
        String[] splitDate = oldDate.split("-");
        return (splitDate[2]+"/"+splitDate[1]+"/"+splitDate[0]);
    }

    public History checkoutConversor(CheckoutHistory checkoutHistory, UserDTO userDTO){
        History history = new History();

        //Checagem de usuario
        history = userCreator(checkoutHistory,userDTO);


        Long id = history.getHistory_id();

        //Criacao de purchase
        Purchases purchases = new Purchases();
        purchases = modelMapper.map(checkoutHistory, Purchases.class);
        purchases.setPurchase_id(sequenceGeneratorService.generateSequence(Purchases.SEQUENCE_NAME));
        Long id2 = purchases.getPurchase_id();

        List<Purchases> purchasesList = purchaseRepository.getPurchases(id);
        purchases.setDate(dateFormatter(checkoutHistory.getDate()));
        purchasesList.add(purchases);

        for (Purchases purchasesAdder : purchasesList){
            mongoTemplate.save(purchasesAdder);
            mongoTemplate
                    .update(History.class)
                    .matching(where("history_id").is(history.getHistory_id()))
                    .apply(new Update().push("purchases", purchasesAdder))
                    .first();
        }

        //Criação de produtos
        for (Products products : purchases.getProducts()) {
            products.setProduct_id(sequenceGeneratorService.generateSequence(Products.SEQUENCE_NAME));
            products.setPurchase_id(purchases.getPurchase_id());
            mongoTemplate.save(products);
            mongoTemplate
                .update(Purchases.class)
                .matching(where("purchase_id").is(purchases.getPurchase_id()))
                .apply(new Update().push("products", products))
                .first();
            purchaseRepository.save(purchases);
            productRepository.save(products);
        }

        purchases = purchaseRepository.getPurchasesByPurchaseId(id2);

        PaymentDTO paymentDTO = checkoutHistory.getPaymentMethod();
        PaymentMethod paymentMethod = modelMapper.map(paymentDTO, PaymentMethod.class);
        paymentMethod.setPayment_id(sequenceGeneratorService.generateSequence(PaymentMethod.SEQUENCE_NAME));
        paymentMethod.setPurchase_id(purchases.getPurchase_id());
        purchases.setPayment_Method(paymentMethod);
        mongoTemplate.save(paymentMethod);
        mongoTemplate
                .update(Purchases.class)
                .matching(where("purchase_id").is(purchases.getPurchase_id()))
                .apply(new Update().push("payment_Method", paymentMethod))
                .first();
        purchaseRepository.save(purchases);
        paymentRepository.save(paymentMethod);

        return history;
    }

    public History userCreator(CheckoutHistory checkoutHistory, UserDTO userDTO){
        History history = new History();
        User user;
        if(userRepository.findByUser_id(checkoutHistory.getUser_id()) == null){
            history.setHistory_id(sequenceGeneratorService.generateSequence(History.SEQUENCE_NAME));
        }else{
            user = userRepository.findByUser_id(checkoutHistory.getUser_id());
            history.setHistory_id(user.getHistory_id());
        }
        historyRepository.save(history);
        user = modelMapper.map(userDTO, User.class);
        user.setUser_id(checkoutHistory.getUser_id());
        user.setHistory_id(history.getHistory_id());

        mongoTemplate.save(user);
        mongoTemplate
                .update(History.class)
                .matching(where("history_id").is(history.getHistory_id()))
                .apply(new Update().push("user", user))
                .first();
        userRepository.save(user);

        return history;
    }

    public History postHistory(CheckoutHistory checkoutHistory){
        History history = checkoutConversor(checkoutHistory,userFeign.getCustomer(checkoutHistory.getUser_id()));
        return history;

    }
}

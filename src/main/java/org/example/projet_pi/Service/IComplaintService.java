package org.example.projet_pi.Service;

import org.example.projet_pi.entity.Complaint;

import java.util.List;

public interface IComplaintService {

    //Iservice

    public Complaint addComplaint(Complaint complaint);

    public Complaint updateComplaint(Complaint complaint);

    public void deleteComplaint(Long id);

    public Complaint getComplaintById(Long id);

    public List<Complaint> getAllComplaints();

}

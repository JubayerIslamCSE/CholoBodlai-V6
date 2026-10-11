import com.jubayer.cholobodlai.EXCEPTION.BusinessRuleException;
import com.jubayer.cholobodlai.Model.Emums.Role;
import com.jubayer.cholobodlai.Service.MarketplaceRules;
import com.jubayer.cholobodlai.Service.InputPolicy;
import java.math.BigDecimal;
public class V5BusinessPolicySmoke {
  static int passes=0;
  static void check(boolean b,String name){if(!b)throw new AssertionError(name); passes++;}
  static void invalid(Runnable r,String name){try{r.run();throw new AssertionError("accepted invalid " + name);}catch(BusinessRuleException ok){passes++;}}
  public static void main(String[] args) {
    MarketplaceRules.accountName("Rahim");passes++;
    invalid(()->MarketplaceRules.accountName("x".repeat(121)),"name length");
    invalid(()->MarketplaceRules.phone("12345678901"),"phone invalid");
    invalid(()->MarketplaceRules.password("123456"),"short password");
    invalid(()->MarketplaceRules.password("X".repeat(101)),"long password");
    invalid(()->MarketplaceRules.email("name@no"),"email invalid");
    invalid(()->MarketplaceRules.registration("abc"),"truck reg short");
    MarketplaceRules.phone("01712345678");passes++;
    MarketplaceRules.registration("DHAKA METRO-12345");passes++;
    MarketplaceRules.bid(new BigDecimal("123.45"));passes++;
    invalid(()->MarketplaceRules.bid(new BigDecimal("0")),"zero bid");
    invalid(()->MarketplaceRules.bid(new BigDecimal("50000000.01")),"bid large");
    invalid(()->MarketplaceRules.bid(new BigDecimal("1.001")),"bid fraction");
    invalid(()->MarketplaceRules.review(5,"x".repeat(501)),"review long");
    invalid(()->MarketplaceRules.review(0,"ok"),"stars invalid");
    check(MarketplaceRules.review(5," nice ").equals("nice"),"review trimmed");
    check(MarketplaceRules.canSeeTrip(Role.ADMIN,"a","s",false,false),"admin can see trip");
    check(!MarketplaceRules.canSeeTrip(Role.SHIPPER,"s2","s",true,false),"other shipper cannot see trip");
    check(!MarketplaceRules.canSeeTrip(Role.OWNER,"o2","s",false,false),"other owner cannot see closed trip");
    check(MarketplaceRules.canSeeTrip(Role.OWNER,"o","s",false,true),"winner can see assigned trip");
    check(!MarketplaceRules.canBidWithTruck(Role.OWNER,"o2","o"),"owner cannot use other truck");
    check(!MarketplaceRules.canBidWithTruck(Role.SHIPPER,"s","o"),"shipper cannot bid");
    check(!MarketplaceRules.canAcceptBid(Role.SHIPPER,"other","shipper"),"other shipper cannot accept");
    check(MarketplaceRules.canAcceptBid(Role.SHIPPER,"shipper","shipper"),"shipper can accept own");
    check(!MarketplaceRules.canSeeMatchedBooking(Role.OWNER,"other","s","winner"),"other owner cannot see contact");
    check(MarketplaceRules.canSeeMatchedBooking(Role.OWNER,"winner","s","winner"),"winner can see contact");
    invalid(()->InputPolicy.searchTerm("x".repeat(81)),"query too long");
    check(InputPolicy.searchTerm("   DHAKA ").equals("dhaka"),"query normalized");
    check(InputPolicy.statusTerm("completed",java.util.Set.of("COMPLETED")).equals("COMPLETED"),"status normalized");
    invalid(()->InputPolicy.statusTerm("invalid",java.util.Set.of("OPEN")),"unknown status");
    System.out.println("PASS: "+passes+" isolated Java validation and access assertions");
  }
}

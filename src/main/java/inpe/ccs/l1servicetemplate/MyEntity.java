package inpe.ccs.l1servicetemplate;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

/** Replace this sample entity with the service domain. */
@Entity
@Getter
@Setter
public class MyEntity extends PanacheEntity {
    private String field;
}

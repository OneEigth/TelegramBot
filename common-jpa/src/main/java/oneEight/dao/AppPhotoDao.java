package oneEight.dao;

import oneEight.entity.AppPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppPhotoDao extends JpaRepository<AppPhoto, Long> {
}

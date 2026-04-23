package dev.kaldiroglu.layered.ayvalikbank.repository;

import dev.kaldiroglu.layered.ayvalikbank.model.Settings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettingsRepository extends JpaRepository<Settings, String> {
}

package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.ModalEffect
import com.wingedsheep.rulesengine.ability.OnEnterBattlefield
import com.wingedsheep.rulesengine.ability.TapUntapEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

/**
 * Glamermite
 *
 * {2}{U} Creature — Faerie Rogue 2/2
 * Flash
 * Flying
 * When this creature enters, choose one —
 * • Tap target creature.
 * • Untap target creature.
 */
object Glamermite {
    val definition = CardDefinition.creature(
        name = "Glamermite",
        manaCost = ManaCost.parse("{2}{U}"),
        subtypes = setOf(Subtype.FAERIE, Subtype.ROGUE),
        power = 2,
        toughness = 2,
        keywords = setOf(Keyword.FLASH, Keyword.FLYING),
        oracleText = "Flash\nFlying\nWhen this creature enters, choose one —\n" +
                "• Tap target creature.\n" +
                "• Untap target creature.",
        metadata = ScryfallMetadata(
            collectorNumber = "50",
            rarity = Rarity.COMMON,
            artist = "Jarel Threat",
            imageUri = "https://cards.scryfall.io/normal/front/a/a/aa6a6a6a-6a6a-6a6a-6a6a-6a6a6a6a6a6a.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Glamermite") {
        keywords(Keyword.FLASH, Keyword.FLYING)

        // ETB: Choose one - tap or untap target creature
        triggered(
            trigger = OnEnterBattlefield(),
            effect = ModalEffect(
                modes = listOf(
                    TapUntapEffect(
                        target = EffectTarget.TargetCreature,
                        tap = true
                    ),
                    TapUntapEffect(
                        target = EffectTarget.TargetCreature,
                        tap = false
                    )
                ),
                chooseCount = 1
            )
        )
    }
}

package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Dungeon Geists
 * {2}{U}{U}
 * Creature — Spirit
 * Flying
 * When this creature enters, tap target creature an opponent controls. That creature doesn't
 * untap during its controller's untap step for as long as you control this creature.
 * 3/3
 */
val DungeonGeists = card("Dungeon Geists") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Spirit"
    power = 3
    toughness = 3
    oracleText = "Flying\n" +
        "When this creature enters, tap target creature an opponent controls. That creature doesn't " +
        "untap during its controller's untap step for as long as you control this creature."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.Tap(creature) then
            Effects.GrantKeyword(
                AbilityFlag.DOESNT_UNTAP,
                creature,
                Duration.WhileYouControlSource("Dungeon Geists"),
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "36"
        artist = "Nils Hamm"
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b715da2e-c816-4c14-8522-811c97c66fed.jpg?1783940842"
    }
}

package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Temur Battle Rage
 * {1}{R}
 * Instant
 * Target creature gains double strike until end of turn.
 * Ferocious — That creature also gains trample until end of turn if you control a creature with
 * power 4 or greater.
 *
 * The ferocious check is made once, as the spell resolves (Force Away's shape); trample then lasts
 * the turn regardless of what happens to your board afterwards.
 */
val TemurBattleRage = card("Temur Battle Rage") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Target creature gains double strike until end of turn.\n" +
        "Ferocious — That creature also gains trample until end of turn if you control a creature with power 4 or greater."

    spell {
        val creature = target(TargetFilter.Creature)

        effect = Effects.GrantKeyword(Keyword.DOUBLE_STRIKE, creature) then
            Effects.If(
                condition = Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Creature.powerAtLeast(4)),
                then = Effects.GrantKeyword(Keyword.TRAMPLE, creature)
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "116"
        artist = "Jaime Jones"
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8a1dd786-f4e3-4951-b87b-82442edec016.jpg?1783938685"
        ruling(
            "2021-03-19",
            "Temur Battle Rage checks whether you control a creature with power 4 or greater as it resolves. If you do, the target creature has trample until end of turn even if you no longer control a creature with power 4 or greater later in the turn.",
        )
    }
}

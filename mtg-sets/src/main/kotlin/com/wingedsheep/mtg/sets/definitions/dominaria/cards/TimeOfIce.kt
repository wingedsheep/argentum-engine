package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Time of Ice
 * {3}{U}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I, II — Tap target creature an opponent controls. It doesn't untap during its controller's
 *          untap step for as long as you control this Saga.
 * III — Return all tapped creatures to their owners' hands.
 */
val TimeOfIce = card("Time of Ice") {
    manaCost = "{3}{U}"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I, II — Tap target creature an opponent controls. It doesn't untap during its controller's untap step for as long as you control Time of Ice.\n" +
        "III — Return all tapped creatures to their owners' hands."

    sagaChapter(1) {
        val creature = target("creature an opponent controls", Targets.CreatureOpponentControls)
        effect = Effects.Tap(creature) then
            GrantKeywordEffect(AbilityFlag.DOESNT_UNTAP.name, creature, Duration.WhileSourceOnBattlefield("Time of Ice"))
    }

    sagaChapter(2) {
        val creature = target("creature an opponent controls", Targets.CreatureOpponentControls)
        effect = Effects.Tap(creature) then
            GrantKeywordEffect(AbilityFlag.DOESNT_UNTAP.name, creature, Duration.WhileSourceOnBattlefield("Time of Ice"))
    }

    sagaChapter(3) {
        effect = EffectPatterns.returnAllToHand(
            GroupFilter(baseFilter = GameObjectFilter.Creature.tapped())
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "70"
        artist = "Franz Vohwinkel"
        imageUri = "https://cards.scryfall.io/normal/front/e/d/edcd0674-68b9-41ca-ab46-a73dfcbe4149.jpg?1562745151"
        ruling("2018-04-27", "The effects of Time of Ice's first two chapter abilities expire if you lose control of it, even if you immediately regain control of it or cast another Time of Ice.")
        ruling("2018-04-27", "The effect of Time of Ice's final chapter ability returns creatures that are tapped for any reason, not just those tapped by Time of Ice.")
    }
}

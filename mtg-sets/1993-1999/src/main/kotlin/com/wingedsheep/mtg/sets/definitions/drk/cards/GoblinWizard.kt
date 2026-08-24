package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Goblin Wizard
 * {2}{R}{R}
 * Creature — Goblin Wizard
 * 1/1
 * {T}: You may put a Goblin permanent card from your hand onto the battlefield.
 * {R}: Target Goblin gains protection from white until end of turn.
 *
 * "Goblin permanent card", not "Goblin creature card" — the filter is the Goblin subtype on any
 * permanent card, so a Goblin artifact or enchantment from a later set would come down too, exactly
 * as printed. `putFromHand`'s `ChooseUpTo(1)` carries the "you may": an empty hand is a no-op, not a
 * failed activation.
 *
 * "Target Goblin" likewise names no card type and no controller — any Goblin permanent on the
 * battlefield, including an opponent's, and including the Wizard himself.
 */
val GoblinWizard = card("Goblin Wizard") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Wizard"
    power = 1
    toughness = 1
    oracleText = "{T}: You may put a Goblin permanent card from your hand onto the battlefield.\n" +
        "{R}: Target Goblin gains protection from white until end of turn."

    activatedAbility {
        cost = Costs.Tap
        effect = Patterns.Hand.putFromHand(
            filter = GameObjectFilter.Permanent.withSubtype(Subtype.GOBLIN),
            count = 1,
            prompt = "Put a Goblin permanent card onto the battlefield?",
        )
        description = "{T}: You may put a Goblin permanent card from your hand onto the battlefield."
    }

    activatedAbility {
        cost = Costs.Mana("{R}")
        target = TargetObject(filter = TargetFilter.Permanent.withSubtype(Subtype.GOBLIN))
        effect = Effects.GrantProtectionFromColor(Color.WHITE)
        description = "{R}: Target Goblin gains protection from white until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "69"
        artist = "Daniel Gelon"
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9b73dfb4-d930-4a89-b621-129dd9f6328c.jpg?1783947934"
    }
}

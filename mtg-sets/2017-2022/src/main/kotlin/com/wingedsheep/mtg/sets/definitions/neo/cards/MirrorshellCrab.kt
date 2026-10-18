package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CounterTarget
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Mirrorshell Crab — Kamigawa: Neon Dynasty #63 (canonical printing)
 * {5}{U}{U} · Artifact Creature — Crab · 5/7
 *
 * Ward {3}
 * Channel — {2}{U}, Discard this card: Counter target spell or ability unless its controller pays {3}.
 *
 * The channel ability's payer is the target's controller: the caster of a spell, or the controller
 * of an activated or triggered ability.
 */
val MirrorshellCrab = card("Mirrorshell Crab") {
    manaCost = "{5}{U}{U}"
    colorIdentity = "U"
    typeLine = "Artifact Creature — Crab"
    power = 5
    toughness = 7
    oracleText = "Ward {3} (Whenever this creature becomes the target of a spell or ability an opponent " +
        "controls, counter it unless that player pays {3}.)\n" +
        "Channel — {2}{U}, Discard this card: Counter target spell or ability unless its controller pays {3}."

    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{3}")))

    // Channel — {2}{U}, Discard this card (from hand): counter target spell or ability unless its
    // controller pays {3}.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{U}"), Costs.DiscardSelf)
        activateFromZone = Zone.HAND
        target(TargetFilter.SpellOrAbilityOnStack)
        effect = Effects.CounterUnlessPays("{3}", target = CounterTarget.SpellOrAbility)
        description = "Channel — {2}{U}, Discard this card: Counter target spell or ability unless its controller pays {3}."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "63"
        artist = "Cristi Balanescu"
        imageUri = "https://cards.scryfall.io/normal/front/0/3/0394c8df-2e8a-4477-93b7-569934d7b936.jpg?1783923900"
        ruling("2022-02-18", "If a player casts a spell that targets multiple permanents their opponent controls with ward, each of those ward abilities will trigger. If that player doesn't pay for all of them, the spell will be countered.")
        ruling("2022-02-18", "Discarding the card is part of the cost to activate a channel ability.")
        ruling("2022-02-18", "If a channel ability requires a target, you may not activate it without a target just to discard the card.")
    }
}

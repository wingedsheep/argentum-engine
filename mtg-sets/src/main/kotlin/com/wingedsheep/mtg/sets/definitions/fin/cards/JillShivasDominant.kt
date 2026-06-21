package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.effects.ReturnFace
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Jill, Shiva's Dominant // Shiva, Warden of Ice
 * {2}{U} — Legendary Creature — Human Noble Warrior 2/2
 * //  — Legendary Enchantment Creature — Saga Elemental 4/5
 *
 * Front — Jill, Shiva's Dominant:
 *   When Jill enters, return up to one other target nonland permanent to its owner's hand.
 *   {3}{U}{U}, {T}: Exile Jill, then return it to the battlefield transformed under its owner's
 *   control. Activate only as a sorcery.
 *
 * Back — Shiva, Warden of Ice (eikon Saga):
 *   (As this Saga enters and after your draw step, add a lore counter.)
 *   I, II — Mesmerize — Target creature can't be blocked this turn.
 *   III — Cold Snap — Tap all lands your opponents control. Exile Shiva, then return it to the
 *   battlefield (front face up).
 */
private val ShivaWardenOfIce = card("Shiva, Warden of Ice") {
    manaCost = ""
    colorIdentity = "U"
    typeLine = "Legendary Enchantment Creature — Saga Elemental"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter.)\n" +
        "I, II — Mesmerize — Target creature can't be blocked this turn.\n" +
        "III — Cold Snap — Tap all lands your opponents control. Exile Shiva, then return it to " +
        "the battlefield (front face up)."
    power = 4
    toughness = 5

    // I, II — Mesmerize — Target creature can't be blocked this turn.
    sagaChapter(1) {
        val t = target("creature", TargetObject(filter = TargetFilter.Creature))
        effect = GrantKeywordEffect(AbilityFlag.CANT_BE_BLOCKED.name, t)
    }
    sagaChapter(2) {
        val t = target("creature", TargetObject(filter = TargetFilter.Creature))
        effect = GrantKeywordEffect(AbilityFlag.CANT_BE_BLOCKED.name, t)
    }

    // III — Cold Snap — Tap all lands your opponents control, then flip Shiva back to Jill.
    sagaChapter(3) {
        effect = Effects.Composite(
            Patterns.Group.tapAll(GroupFilter(GameObjectFilter.Land.opponentControls())),
            Effects.ExileAndReturnTransformed(EffectTarget.Self, ReturnFace.FRONT),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "58"
        artist = "Arif Wijaya"
        imageUri = "https://cards.scryfall.io/normal/back/1/f/1f163763-4802-4a96-a5bc-f3c381db7b5c.jpg?1748707805"
    }
}

private val JillShivasDominantFront = card("Jill, Shiva's Dominant") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Noble Warrior"
    oracleText = "When Jill enters, return up to one other target nonland permanent to its " +
        "owner's hand.\n" +
        "{3}{U}{U}, {T}: Exile Jill, then return it to the battlefield transformed under its " +
        "owner's control. Activate only as a sorcery."
    power = 2
    toughness = 2

    // When Jill enters, return up to one other target nonland permanent to its owner's hand.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target(
            "nonland permanent",
            TargetObject(
                optional = true,
                filter = TargetFilter(GameObjectFilter.NonlandPermanent, excludeSelf = true),
            ),
        )
        effect = Effects.ReturnToHand(t)
    }

    // {3}{U}{U}, {T}: Exile Jill, then return it transformed. Activate only as a sorcery.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}{U}{U}"), Costs.Tap)
        timing = TimingRule.SorcerySpeed
        effect = Effects.ExileAndReturnTransformed()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "58"
        artist = "Arif Wijaya"
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1f163763-4802-4a96-a5bc-f3c381db7b5c.jpg?1748707805"
    }
}

val JillShivasDominant: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = JillShivasDominantFront,
    backFace = ShivaWardenOfIce,
)

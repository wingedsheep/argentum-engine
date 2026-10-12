package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unearth
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cityscape Leveler — The Brothers' War #233
 * {8} · Artifact Creature — Construct · Mythic
 * 8/8
 *
 * Trample
 * When you cast this spell and whenever this creature attacks, destroy up to one target nonland
 * permanent. Its controller creates a tapped Powerstone token.
 * Unearth {8}
 *
 * "When you cast this spell and whenever ~ attacks" is two triggers sharing one effect. The
 * destroy-and-replace runs inside `ForEachTarget`, so choosing no target creates no Powerstone
 * (a bare `TargetController` falls back to the ability's controller), and "its controller" reads
 * the destroyed permanent's controller — an indestructible target still hands its controller one.
 */
private const val LEVEL_TEXT =
    "destroy up to one target nonland permanent. Its controller creates a tapped Powerstone token."

private val levelEffect = Effects.ForEachTarget(
    Effects.Destroy(EffectTarget.ContextTarget(0)),
    Effects.CreatePowerstone(tapped = true, controller = EffectTarget.TargetController),
)

val CityscapeLeveler = card("Cityscape Leveler") {
    manaCost = "{8}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Construct"
    power = 8
    toughness = 8
    oracleText = "Trample\n" +
        "When you cast this spell and whenever this creature attacks, destroy up to one target nonland " +
        "permanent. Its controller creates a tapped Powerstone token. (It's an artifact with \"{T}: Add {C}. " +
        "This mana can't be spent to cast a nonartifact spell.\")\n" +
        "Unearth {8} ({8}: Return this card from your graveyard to the battlefield. It gains haste. Exile it " +
        "at the beginning of the next end step or if it would leave the battlefield. Unearth only as a sorcery.)"

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.isCast()
        target(TargetFilter.NonlandPermanent, optional = true)
        effect = levelEffect
        description = "When you cast this spell, $LEVEL_TEXT"
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        target(TargetFilter.NonlandPermanent, optional = true)
        effect = levelEffect
        description = "Whenever this creature attacks, $LEVEL_TEXT"
    }

    unearth("{8}")

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "233"
        artist = "Leon Tukker"
        imageUri = "https://cards.scryfall.io/normal/front/6/7/67a87278-4c82-4056-8354-253d86b0ef3d.jpg?1783920019"
        ruling("2022-10-14", "Cityscape Leveler's triggered ability won't trigger when you activate its unearth ability from your graveyard. It will, however, trigger when you attack with Cityscape Leveler that turn.")
        ruling("2022-10-14", "If a permanent returned to the battlefield with unearth would leave the battlefield for any reason, it's exiled instead—unless the spell or ability that's causing the permanent to leave the battlefield is actually trying to exile it! In that case, it succeeds at exiling it.")
    }
}

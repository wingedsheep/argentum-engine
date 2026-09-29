package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nezumi Freewheeler // Hideous Fleshwheeler (March of the Machine #119)
 * {3}{B} Creature — Rat Samurai 3/3 // Creature — Phyrexian Rat 4/5
 *
 * Front — Menace; when it enters, each player mills three cards;
 * "{5}{W/P}: Transform this creature. Activate only as a sorcery."
 *
 * Back — Menace; "When this creature transforms into Hideous Fleshwheeler, put target permanent
 * card with mana value 2 or less from a graveyard onto the battlefield under your control."
 * Any graveyard, so the move carries an explicit `controllerOverride` — a bare move to the
 * battlefield would hand an opponent's card back to its owner.
 */
private val NezumiFreewheelerFront = card("Nezumi Freewheeler") {
    manaCost = "{3}{B}"
    colorIdentity = "BW"
    typeLine = "Creature — Rat Samurai"
    power = 3
    toughness = 3
    oracleText = "Menace\n" +
        "When this creature enters, each player mills three cards. (To mill three cards, a player " +
        "puts the top three cards of their library into their graveyard.)\n" +
        "{5}{W/P}: Transform this creature. Activate only as a sorcery. " +
        "({W/P} can be paid with either {W} or 2 life.)"

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.mill(3, EffectTarget.PlayerRef(Player.Each))
        description = "When this creature enters, each player mills three cards."
    }

    activatedAbility {
        cost = Costs.Mana("{5}{W/P}")
        effect = Effects.Transform(EffectTarget.Self)
        timing = TimingRule.SorcerySpeed
        description = "Transform this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "119"
        artist = "Artur Nakhodkin"
        imageUri = "https://cards.scryfall.io/normal/front/7/6/76b5e289-6bc4-48ee-8d5b-6989bab9f901.jpg?1783917011"
    }
}

private val HideousFleshwheeler = card("Hideous Fleshwheeler") {
    manaCost = ""
    colorIndicator = "BW" // Transformed back face, no mana cost (CR 204).
    colorIdentity = "BW"
    typeLine = "Creature — Phyrexian Rat"
    power = 4
    toughness = 5
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "When this creature transforms into Hideous Fleshwheeler, put target permanent card with " +
        "mana value 2 or less from a graveyard onto the battlefield under your control."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.self.transforms(true)
        val reanimated = target(
            TargetFilter(GameObjectFilter.Permanent.manaValueAtMost(2), zone = Zone.GRAVEYARD)
        )
        effect = Effects.Move(reanimated, Zone.BATTLEFIELD, controllerOverride = EffectTarget.Controller)
        description = "When this creature transforms into Hideous Fleshwheeler, put target permanent " +
            "card with mana value 2 or less from a graveyard onto the battlefield under your control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "119"
        artist = "Artur Nakhodkin"
        flavorText = "Freedom became a distasteful memory, a thing to fear and destroy."
        imageUri = "https://cards.scryfall.io/normal/back/7/6/76b5e289-6bc4-48ee-8d5b-6989bab9f901.jpg?1783917011"
    }
}

val NezumiFreewheeler: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = NezumiFreewheelerFront,
    backFace = HideousFleshwheeler,
)

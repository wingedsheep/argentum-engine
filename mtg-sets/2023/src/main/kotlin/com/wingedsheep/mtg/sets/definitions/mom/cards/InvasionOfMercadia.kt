package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Invasion of Mercadia // Kyren Flamewright — March of the Machine #147.
 * {1}{R} · Battle — Siege · defense 4 // Creature — Goblin Spellshaper 3/3
 *
 * Front: "you may discard a card. If you do, draw two" is a [Effects.May] around
 * [Effects.IfYouDo] — nothing is drawn when the discard didn't happen (including an empty hand).
 * Back: the pump-and-haste group is read on resolution, after the tokens exist, so the tokens
 * are included.
 */
private val InvasionOfMercadiaFront = card("Invasion of Mercadia") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Battle — Siege"
    startingDefense = 4
    oracleText = "(As a Siege enters, choose an opponent to protect it. You and others can attack " +
        "it. When it's defeated, exile it, then cast it transformed.)\n" +
        "When this Siege enters, you may discard a card. If you do, draw two cards."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.May(Effects.IfYouDo(Effects.Discard(1), Effects.DrawCards(2)))
        description = "When this Siege enters, you may discard a card. If you do, draw two cards."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "147"
        artist = "Cristi Balanescu"
        imageUri = "https://cards.scryfall.io/normal/front/4/0/407d6723-bf58-403e-b2ac-ba52c51d356f.jpg?1783916993"
    }
}

private val KyrenFlamewright = card("Kyren Flamewright") {
    manaCost = ""
    colorIdentity = "R"
    colorIndicator = "R"
    typeLine = "Creature — Goblin Spellshaper"
    power = 3
    toughness = 3
    oracleText = "{2}{R}, {T}, Discard a card: Create two 1/1 blue and red Elemental creature tokens. Creatures you control get +1/+0 and gain haste until end of turn."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{R}"), Costs.Tap, Costs.Discard())
        effect = Effects.CreateToken(
            count = 2,
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLUE, Color.RED),
            creatureTypes = setOf("Elemental"),
            imageUri = "https://cards.scryfall.io/normal/front/2/8/28a7a9b0-d823-4b34-829f-ade81fc141e0.jpg?1783916668",
        ) then Patterns.Group.pumpAndGrantToAll(
            power = 1,
            toughness = 0,
            keyword = Keyword.HASTE,
            filter = GroupFilter(GameObjectFilter.Creature.youControl()),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "147"
        artist = "Cristi Balanescu"
        flavorText = "Inspired by tales of the legendary hero Squee, many young Kyren goblins strove to emulate his magical skill and courage in the face of danger."
        imageUri = "https://cards.scryfall.io/normal/back/4/0/407d6723-bf58-403e-b2ac-ba52c51d356f.jpg?1783916993"
        ruling("2023-04-14", "The set of creatures affected by Kyren Flamewright's ability is determined as the ability resolves. The Elemental tokens will get the bonuses, but creatures you begin to control later in the turn and noncreature permanents that become creatures later in the turn will not.")
    }
}

val InvasionOfMercadia: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = InvasionOfMercadiaFront,
    backFace = KyrenFlamewright,
)

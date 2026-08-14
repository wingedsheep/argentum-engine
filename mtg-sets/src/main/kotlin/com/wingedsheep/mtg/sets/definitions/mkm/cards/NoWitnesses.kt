package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.ForEachPlayerEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * No Witnesses — Murders at Karlov Manor #27
 * {2}{W}{W} · Sorcery
 *
 * Each player who controls the most creatures investigates. Then destroy all creatures.
 *
 * A wrath that pays the player who was ahead on board — including you, if you're the one who was
 * ahead — with a Clue apiece.
 *
 * "Each player who controls the most creatures" is the Outpace Oblivion shape: one
 * [ForEachPlayerEffect] over [Player.Each] whose body is a [ConditionalEffect]. Inside the loop
 * the controller is rebound to the iterated player, so
 * [Conditions.PlayerControlsMostPermanents] asks about *that* player and the Clue lands in front
 * of them. Ties all qualify (the condition is "most, or tied for most"), which is why this can
 * hand out several Clues — or, in a mirrored board, one to everybody.
 *
 * Order matters and is printed: every qualifying player investigates *before* the wipe, so the
 * counts are read off the pre-destruction battlefield. Sequential iteration is safe here because
 * creating a Clue can't change anyone's creature count.
 *
 * The counts come from the projected battlefield, so an animated land or a creature that has lost
 * its types is counted the way the board actually reads.
 */
val NoWitnesses = card("No Witnesses") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Each player who controls the most creatures investigates. Then destroy all " +
        "creatures. (To investigate, create a Clue token. It's an artifact with \"{2}, Sacrifice " +
        "this token: Draw a card.\")"

    spell {
        effect = Effects.Composite(
            ForEachPlayerEffect(
                players = Player.Each,
                effects = listOf(
                    ConditionalEffect(
                        condition = Conditions.PlayerControlsMostPermanents(
                            Player.You,
                            GameObjectFilter.Creature,
                        ),
                        effect = Effects.Investigate(controller = EffectTarget.Controller),
                    ),
                ),
            ),
            Effects.DestroyAll(GameObjectFilter.Creature),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "27"
        artist = "Michele Giorgi"
        flavorText = "When cunning fails, there's always violence."
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f98db67a-c39c-45a8-ae21-85133be46ed5.jpg?1783912920"

        ruling(
            "2024-02-02",
            "Clue is an artifact type. Even though it appears on some cards with other permanent " +
                "types, it's never a creature type, a land type, or anything but an artifact type."
        )
    }
}

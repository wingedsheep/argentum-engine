package com.wingedsheep.mtg.sets.definitions.c18.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Treasure Nabber
 * {2}{R}
 * Creature — Goblin Rogue
 * 3/2
 *
 * Whenever an opponent taps an artifact for mana, gain control of that artifact until the end of
 * your next turn.
 *
 * [Triggers.anOpponent] `.tapsPermanentForMana(Artifact)` watches an opponent's mana-ability taps
 * (a mana ability with {T} in its cost — tapping an artifact to improvise is not that), and the
 * tapped artifact is the triggering entity the control change acts on. The artifact stays tapped,
 * and if it has left the battlefield (a Treasure sacrificed for its mana) there is nothing to take.
 * [Duration.EndOfYourNextTurn] runs through the end of the Nabber controller's next turn, whether
 * the tap happened on their turn or an opponent's.
 */
val TreasureNabber = card("Treasure Nabber") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Rogue"
    power = 3
    toughness = 2
    oracleText = "Whenever an opponent taps an artifact for mana, gain control of that artifact until " +
        "the end of your next turn."

    triggeredAbility {
        trigger = Triggers.anOpponent.tapsPermanentForMana(GameObjectFilter.Artifact)
        effect = Effects.GainControl(EffectTarget.TriggeringEntity, Duration.EndOfYourNextTurn)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "27"
        artist = "Alex Konstad"
        flavorText = "The Law of Givesies Backsies is respected by every noble pursuer of shiny things."
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6c6049a0-2c13-41ae-8c73-978be8b13031.jpg?1783934335"
        ruling(
            "2018-07-13",
            "An artifact is \"tapped for mana\" when a player activates a mana ability of an artifact that includes {T} (the tap symbol). Notably, tapping an artifact to pay for a spell with the improvise keyword is not tapping that artifact for mana."
        )
        ruling(
            "2018-07-13",
            "If the artifact leaves the battlefield, Treasure Nabber doesn't nab it from the zone it moved to."
        )
        ruling(
            "2018-07-13",
            "The artifact remains tapped when you gain control of it."
        )
        ruling(
            "2018-07-13",
            "If an opponent taps an artifact for mana during your turn, you'll gain control of it and keep control of it until the end of your next turn."
        )
        ruling(
            "2018-07-13",
            "If you control multiple Treasure Nabbers, their effects all expire at the same time. You won't keep an artifact for any longer."
        )
        ruling(
            "2018-07-13",
            "If multiple players each control Treasure Nabbers, an artifact tapped for mana will make quite a journey. For example, if player A taps an artifact for mana during their turn, and players B and C are the next players in turn order who control Treasure Nabbers, C's ability resolves before B and C gains control of the artifact. Then B gains control of the artifact. B keeps it during their turn, and then C regains control of the artifact once B's effect expires, then A will have it back after C's expires. However, if B or C taps the artifact for mana, one of them will take it from the other. You may need to pay very careful attention to which effects are still in effect, which have expired, and who owns what."
        )
        ruling(
            "2018-07-13",
            "In a multiplayer game, if a player leaves the game, all cards that player owns leave as well. If that player controls any nabbed treasures, the effect giving that player control of those artifacts ends."
        )
    }
}

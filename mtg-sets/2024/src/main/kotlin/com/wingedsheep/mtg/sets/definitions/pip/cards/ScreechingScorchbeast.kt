package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Screeching Scorchbeast — the mill payoff is a batch trigger ("one or more nonland cards are
 * milled", once per mill instruction, CR 603.2c) whose "that many" counts the captured nonland
 * cards. "Do this only once each turn" is `effectOncePerTurn` (CR 603.2h): declining the "may"
 * leaves the ability free to trigger again this turn; creating the tokens spends it.
 */
val ScreechingScorchbeast = card("Screeching Scorchbeast") {
    manaCost = "{4}{B}{B}"
    typeLine = "Creature — Bat Mutant"
    power = 5
    toughness = 5
    oracleText = "Flying, menace\n" +
        "Whenever this creature attacks, each player gets two rad counters.\n" +
        "Whenever one or more nonland cards are milled, you may create that many 2/2 black Zombie Mutant creature tokens. Do this only once each turn."

    keywords(Keyword.FLYING, Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.ForEachPlayer(
            Player.Each,
            Effects.AddCounters(CounterType.RAD, 2, EffectTarget.Controller)
        )
    }

    triggeredAbility {
        trigger = Triggers.anyPlayer.mills(GameObjectFilter.Nonland, batch = true)
        optional = true
        effectOncePerTurn = true
        description = "Whenever one or more nonland cards are milled, you may create that many 2/2 " +
            "black Zombie Mutant creature tokens. Do this only once each turn."
        effect = Effects.CreateToken(
            count = DynamicAmounts.distinctEntitiesIn(CollectionSlot.TriggerCaptured),
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie", "Mutant"),
            imageUri = "https://cards.scryfall.io/normal/front/b/a/ba45bc9c-8257-43f8-9ba1-116b9fea90f4.jpg?1783912046"
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "49"
        artist = "Nino Is"
        imageUri = "https://cards.scryfall.io/normal/front/9/6/967dbb24-3073-4c33-bfdf-b3eb9e8fe443.jpg?1783912407"
        ruling(
            "2024-03-08",
            "If more than one player is instructed to mill at the same time (by the activated ability of Raul, Trouble Shooter, for example), as long as at least one nonland card is milled, Screeching Scorchbeast's last ability will trigger just once."
        )
        ruling(
            "2024-03-08",
            "\"Do this only once each turn\" lets you choose whether or not to create the appropriate number of 2/2 black Zombie Mutant creature tokens as the triggered ability resolves. If you choose not to create tokens, the ability will trigger again the next time the condition is met. Once you choose to do so, the ability will no longer trigger for the rest of the turn."
        )
    }
}

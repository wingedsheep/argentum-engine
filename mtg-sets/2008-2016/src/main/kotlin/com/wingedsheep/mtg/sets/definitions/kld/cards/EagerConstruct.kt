package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Eager Construct
 * {2}
 * Artifact Creature — Construct
 * 2/2
 * When this creature enters, each player may scry 1.
 *
 * `ForEachPlayer(ActivePlayerFirst)` rebinds the resolving controller to each player in turn
 * order, so every player answers their own "may" and scries their own library. Declining is not
 * scrying, so no "whenever you scry" trigger fires for that player.
 */
val EagerConstruct = card("Eager Construct") {
    manaCost = "{2}"
    typeLine = "Artifact Creature — Construct"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, each player may scry 1. (To scry 1, look at the top " +
        "card of your library, then you may put that card on the bottom.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ForEachPlayer(
            Player.ActivePlayerFirst,
            Effects.May(
                effect = Effects.Scry(1),
                decisionMaker = EffectTarget.PlayerRef(Player.You),
                descriptionOverride = "Scry 1?",
            ),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "209"
        artist = "Craig J Spearing"
        flavorText = "Loyal to a fault, it anticipates its master's every need."
        imageUri = "https://cards.scryfall.io/normal/front/6/6/66f8ae7b-02a5-45ce-8a63-aa880b1e582a.jpg?1783937157"
        ruling(
            "2020-08-07",
            "If multiple players are instructed to scry at once, such as while resolving Eager " +
                "Construct's triggered ability, those players each look at the top card of their " +
                "library at the same time, then they choose in turn order whether to put those " +
                "cards on the top or bottom of their library."
        )
    }
}

package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Biblioplex Kraken
 * {4}{U}
 * Creature — Kraken
 * 4/5
 *
 * When this creature enters, scry 3.
 * Whenever this creature attacks, you may return another creature you control to its owner's
 * hand. If you do, this creature can't be blocked this turn.
 *
 * The bounce doesn't target (per the 2022-12-02 ruling, the choice is made on resolution), so it is
 * a gather → `chooseExactly(1)` → `toHand` pipeline inside [Effects.IfYouDo]: the unblockability
 * grant runs only when a creature actually reached a hand (`SuccessCriterion.Auto` reads the
 * pipeline's terminal move). With no other creature, nothing moves and the payoff is skipped.
 */
val BiblioplexKraken = card("Biblioplex Kraken") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Kraken"
    power = 4
    toughness = 5
    oracleText = "When this creature enters, scry 3. (Look at the top three cards of your library, then " +
        "put any number of them on the bottom and the rest on top in any order.)\n" +
        "Whenever this creature attacks, you may return another creature you control to its owner's " +
        "hand. If you do, this creature can't be blocked this turn."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.scry(3)
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.May(
            Effects.IfYouDo(
                action = Effects.Pipeline {
                    val candidates = gather(
                        CardSource.BattlefieldMatching(
                            filter = GameObjectFilter.Creature,
                            player = Player.You,
                            excludeSelf = true
                        )
                    )
                    val bounced = chooseExactly(
                        1,
                        from = candidates,
                        prompt = "Return another creature you control to its owner's hand",
                        useTargetingUI = true
                    )
                    toHand(bounced)
                },
                then = Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, EffectTarget.Self)
            )
        )
        description = "Whenever this creature attacks, you may return another creature you control to " +
            "its owner's hand. If you do, this creature can't be blocked this turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "10"
        artist = "Caio Monteiro"
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1f69f0bf-6626-4418-9335-b79d75c99df1.jpg?1783919194"
        ruling(
            "2022-12-02",
            "You choose whether to return a creature and which creature to return as the triggered " +
                "ability resolves. This doesn't target any creature."
        )
    }
}

package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Lord Skitter's Butcher
 * {2}{B}
 * Creature — Rat Peasant
 * 2/3
 *
 * When this creature enters, choose one —
 * • Create a 1/1 black Rat creature token with "This token can't block."
 * • You may sacrifice another creature. If you do, scry 2, then draw a card.
 * • Creatures you control gain menace until end of turn.
 *
 * None of the modes target, so the mode is picked at resolution. Mode 2 is "**If** you do" (not
 * "When you do"), so the payoff happens in the same resolution rather than as a reflexive trigger:
 * a gather → choose-up-to-one → sacrifice pipeline carries the optional (choosing nothing *is* the
 * "you may" no), and [Effects.IfYouDo] gates the scry+draw on a creature actually being sacrificed
 * — the Midgar, City of Mako shape. `excludeSelf = true` enforces "another", and the choice is made
 * on the battlefield (`useTargetingUI`) rather than in an overlay.
 */
val LordSkittersButcher = card("Lord Skitter's Butcher") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Rat Peasant"
    oracleText = "When this creature enters, choose one —\n" +
        "• Create a 1/1 black Rat creature token with \"This token can't block.\"\n" +
        "• You may sacrifice another creature. If you do, scry 2, then draw a card.\n" +
        "• Creatures you control gain menace until end of turn."
    power = 2
    toughness = 3

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = ModalEffect.chooseOne(
            Mode(
                effect = woeRatToken(),
                description = "Create a 1/1 black Rat creature token with \"This token can't block.\""
            ),
            Mode(
                effect = Effects.IfYouDo(
                    action = Effects.Pipeline {
                        val fodder = gather(
                            GameObjectFilter.Creature,
                            player = Player.You,
                            excludeSelf = true
                        )
                        val chosen = chooseUpTo(
                            1,
                            from = fodder,
                            useTargetingUI = true,
                            prompt = "Choose another creature to sacrifice"
                        )
                        sacrifice(chosen)
                    },
                    ifYouDo = Effects.Composite(
                        Patterns.Library.scry(2),
                        Effects.DrawCards(1)
                    )
                ),
                description = "You may sacrifice another creature. If you do, scry 2, then draw a card."
            ),
            Mode(
                effect = Patterns.Group.grantKeywordToAll(
                    Keyword.MENACE,
                    GroupFilter.AllCreaturesYouControl
                ),
                description = "Creatures you control gain menace until end of turn."
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "99"
        artist = "Leesha Hannigan"
        imageUri = "https://cards.scryfall.io/normal/front/2/1/21b31d2b-ef66-4e16-a75e-4e27eb5ebfe9.jpg?1783915105"
    }
}

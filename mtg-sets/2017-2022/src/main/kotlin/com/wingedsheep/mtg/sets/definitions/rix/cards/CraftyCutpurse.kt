package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.RedirectTokenCreationController
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Crafty Cutpurse
 * {3}{U}
 * Creature — Human Pirate
 * 2/2
 * Flash
 * When this creature enters, each token that would be created under an opponent's control this
 * turn is created under your control instead.
 *
 * The trigger grants a [RedirectTokenCreationController] for the turn. The grant lives on the game
 * state, not on Cutpurse, so it keeps working after Cutpurse leaves the battlefield. The engine
 * applies it before every other token replacement, which is what the first ruling asks for: an
 * opponent's Anointed Procession no longer doubles the tokens, and yours does.
 */
val CraftyCutpurse = card("Crafty Cutpurse") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Pirate"
    power = 2
    toughness = 2
    oracleText = "Flash\nWhen this creature enters, each token that would be created under an " +
        "opponent's control this turn is created under your control instead."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GrantReplacementEffect(
            replacement = RedirectTokenCreationController(
                appliesTo = EventPattern.TokenCreationEvent(controller = Player.EachOpponent)
            ),
            target = EffectTarget.Self,
            duration = Duration.EndOfTurn,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "33"
        artist = "Grzegorz Rutkowski"
        flavorText = "Possession is eleven-tenths of the pirate code."
        imageUri = "https://cards.scryfall.io/normal/front/a/c/ac6afc4c-3c41-44e8-ad8c-186c277267a1.jpg?1783935328"

        ruling("2018-01-19", "Crafty Cutpurse's replacement effect is applied before any other replacement effects that would also modify how the token enters the battlefield. For example, if an opponent controls Anointed Procession, your Crafty Cutpurse's effect applies before that of Anointed Procession and you won't get twice as many tokens.")
        ruling("2018-01-19", "Crafty Cutpurse won't retroactively change the control of tokens that have already entered the battlefield.")
        ruling("2018-01-19", "If a token would be created tapped and attacking, but the token's controller isn't an attacking player, that token is created tapped but not attacking. If a token would be created blocking a creature, but the token's controller isn't a defending player, that token is created but isn't blocking.")
        ruling("2018-01-19", "If the effect that creates the token also creates a delayed triggered ability, Crafty Cutpurse doesn't change who controls that ability. For example, if your opponent activates the ability of Kiki-Jiki, Mirror Breaker, that opponent controls the delayed triggered ability. When the ability triggers, that player can't sacrifice the token, so it remains on the battlefield.")
        ruling("2018-01-19", "On the other hand, if the effect that creates the token grants a triggered ability to the token, the player who controls the token at the time the ability triggers will be the player who controls that ability. For example, if your opponent activates the second ability of Jace, Cunning Castaway, the token has the ability. If the ability triggers, you'll control it, so you'll have to sacrifice the token.")
        ruling("2018-01-19", "If two or more players each have resolved Crafty Cutpurse's triggered ability and a token would be created, the token's would-be controller chooses one of the applicable Crafty Cutpurse effects to apply. Then the new would-be controller of the token repeats this process among the remaining Crafty Cutpurse effects, and so on, until there are no more possible such effects to apply. Each effect can be applied to the token only once this way.")
        ruling("2018-01-19", "The above procedure means that if each player in a two-player game has resolved Crafty Cutpurse's triggered ability and one would create a token, it really will enter the battlefield under that player's control. The token would enter the battlefield under player A's control, so player B's effect affects it. Now that token would enter the battlefield under player B's control, so player A's effect affects it. Each replacement effect has now been used, so the token will enter the battlefield under player A's control.")
    }
}

package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tergrid, God of Fright // Tergrid's Lantern — Kaldheim #112 (canonical printing)
 * Legendary Creature — God 4/5 // Legendary Artifact · Modal double-faced card
 *
 * Front: "Menace. Whenever an opponent sacrifices a nontoken permanent or discards a permanent
 * card, you may put that card from a graveyard onto the battlefield under your control."
 *
 * The "or" trigger is two triggered abilities sharing one effect: the sacrifice half is detected
 * by the sacrifice pass (which doesn't read `Triggers.or`'s `AnyOf`), and no single event is both
 * a sacrifice and a discard, so two abilities fire exactly when the printed one would. Each binds
 * the card as the triggering entity — the sacrificed permanent (now a card in its owner's
 * graveyard — "it doesn't matter which graveyard it goes to") or the discarded card. The return is
 * graveyard-guarded, so a card that has left the graveyard (or that a madness discard sent to
 * exile) stays where it is, per the ruling.
 *
 * Back: "{T}: Target player loses 3 life unless they sacrifice a nonland permanent of their choice
 * or discard a card. {3}{B}: Untap Tergrid's Lantern." — the Starseer Mentor punisher
 * (`PayOrSuffer` over a `Costs.pay.Choice`) aimed at any target player.
 */
private val TergridGodOfFrightFront = card("Tergrid, God of Fright") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — God"
    power = 4
    toughness = 5
    oracleText = "Menace\nWhenever an opponent sacrifices a nontoken permanent or discards a permanent " +
        "card, you may put that card from a graveyard onto the battlefield under your control."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.anOpponent.sacrifices(GameObjectFilter.Permanent.nontoken())
        optional = true
        effect = Effects.PutOntoBattlefieldFromGraveyard(EffectTarget.TriggeringEntity, underYourControl = true)
        description = "Whenever an opponent sacrifices a nontoken permanent, you may put that card " +
            "from a graveyard onto the battlefield under your control."
    }

    triggeredAbility {
        trigger = Triggers.anOpponent.discards(GameObjectFilter.Permanent)
        optional = true
        effect = Effects.PutOntoBattlefieldFromGraveyard(EffectTarget.TriggeringEntity, underYourControl = true)
        description = "Whenever an opponent discards a permanent card, you may put that card " +
            "from a graveyard onto the battlefield under your control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "112"
        artist = "Yongjae Choi"
        flavorText = "Fear is her one true companion."
        imageUri = "https://cards.scryfall.io/normal/front/1/4/14dc88ee-bba9-4625-af0d-89f3762a0ead.jpg?1783928244"
        ruling("2021-02-05", "If an opponent sacrifices a nontoken permanent, it doesn't matter which graveyard it goes to. Tergrid's ability will trigger.")
        ruling("2021-02-05", "If the card leaves the graveyard before Tergrid's ability resolves, you can't put it onto the battlefield, even if it returns to the graveyard before the ability resolves.")
        ruling("2021-02-05", "If an opponent sacrifices a nontoken permanent or discards a permanent card as part of paying the cost of a spell or ability, Tergrid's ability will trigger and goes on the stack on top of that spell or ability. Tergrid's ability will resolve before that spell or ability.")
        ruling("2021-02-05", "If you put an Aura onto the battlefield using Tergrid's ability, you choose what the Aura will enchant just before it enters the battlefield. An Aura put onto the battlefield this way doesn't target the player or permanent it will enchant, so permanents or players with hexproof may be chosen; however, the chosen recipient must be able to legally be enchanted by the Aura, so a player or permanent with protection from one of the Aura's qualities can't be chosen this way. If there's nothing legal for the Aura to enchant, it stays in the graveyard.")
    }
}

private val TergridsLanternBack = card("Tergrid's Lantern") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Artifact"
    oracleText = "{T}: Target player loses 3 life unless they sacrifice a nonland permanent of their " +
        "choice or discard a card.\n{3}{B}: Untap Tergrid's Lantern."

    activatedAbility {
        cost = Costs.Tap
        val player = target(Targets.Player)
        effect = Effects.PayOrSuffer(
            cost = Costs.pay.Choice(
                listOf(
                    Costs.pay.Sacrifice(filter = GameObjectFilter.Nonland),
                    Costs.pay.Discard()
                )
            ),
            suffer = Effects.LoseLife(amount = 3, target = player),
            player = player
        )
        description = "Target player loses 3 life unless they sacrifice a nonland permanent of their " +
            "choice or discard a card."
    }

    activatedAbility {
        cost = Costs.Mana("{3}{B}")
        effect = Effects.Untap(EffectTarget.Self)
        description = "Untap Tergrid's Lantern."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "112"
        artist = "Yongjae Choi"
        flavorText = "When Tergrid was a child, her shadow tried to kill her. Only her lantern keeps it at bay."
        imageUri = "https://cards.scryfall.io/normal/back/1/4/14dc88ee-bba9-4625-af0d-89f3762a0ead.jpg?1783928244"
        ruling("2021-02-05", "The player targeted by the first ability of Tergrid's Lantern can choose to lose 3 life even if they have nonland permanents to sacrifice or cards to discard. They can't choose to sacrifice a nonland permanent if they don't control any, nor can they choose to discard a card if they have none in hand.")
    }
}

val TergridGodOfFright: CardDefinition = CardDefinition.modalDoubleFacedPermanent(
    frontFace = TergridGodOfFrightFront,
    backFace = TergridsLanternBack,
)

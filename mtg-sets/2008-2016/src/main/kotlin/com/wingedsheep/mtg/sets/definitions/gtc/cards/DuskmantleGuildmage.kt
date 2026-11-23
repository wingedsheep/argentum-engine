package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Duskmantle Guildmage — Gatecrash #158
 * {U}{B} · Creature — Human Wizard · 2 / 2
 *
 *  - The first ability creates a global triggered ability until end of turn (False Cure's shape),
 *    so it outlives the Guildmage. Its trigger is Bloodchief Ascension's / Vulturous Zombie's:
 *    a per-card `ZoneChangeEvent` to a graveyard with no `from` zone, nontoken (a token isn't a
 *    card — third ruling), owned by an opponent of the activator (a card goes to its owner's
 *    graveyard). "That player" is [Player.OwnerOfTriggeringEntity]. Each activation creates its
 *    own ability, so two activations make each card cost 2 life (second ruling).
 *  - The second ability is Millstone's targeted mill.
 */
val DuskmantleGuildmage = card("Duskmantle Guildmage") {
    manaCost = "{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 2
    oracleText = "{1}{U}{B}: Whenever a card is put into an opponent's graveyard from anywhere this turn, " +
        "that player loses 1 life.\n" +
        "{2}{U}{B}: Target player mills two cards."

    activatedAbility {
        cost = Costs.Mana("{1}{U}{B}")
        effect = Effects.CreateGlobalTriggeredAbility(
            duration = Duration.EndOfTurn,
            ability = TriggeredAbility.create(
                trigger = Triggers.a(
                    GameObjectFilter.Any
                        .withCardPredicate(CardPredicate.IsNontoken)
                        .ownedByOpponent()
                ).changesZone(to = Zone.GRAVEYARD),
                effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.OwnerOfTriggeringEntity)),
                descriptionOverride = "Whenever a card is put into an opponent's graveyard from anywhere " +
                    "this turn, that player loses 1 life."
            )
        )
        description = "Whenever a card is put into an opponent's graveyard from anywhere this turn, " +
            "that player loses 1 life."
    }

    activatedAbility {
        cost = Costs.Mana("{2}{U}{B}")
        val player = target(Targets.Player)
        effect = Patterns.Library.mill(2, player)
        description = "Target player mills two cards."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "158"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9a1509ff-387e-4ccd-bda0-86e8738a98fb.jpg?1783940109"
        ruling(
            "2013-01-24",
            "The first ability considers each of your opponents. You don't choose just one of them."
        )
        ruling(
            "2013-01-24",
            "Each time the first ability resolves, a delayed triggered ability is created. Whenever a card " +
                "is put into an opponent's graveyard that turn, each of those abilities will trigger. For " +
                "example, if you activate the first ability twice (and let those abilities resolve) and then " +
                "activate the second ability targeting an opponent, that player will lose a total of 4 life."
        )
        ruling(
            "2013-01-24",
            "The delayed triggered ability created won't trigger if a token permanent is put into an " +
                "opponent's graveyard."
        )
    }
}

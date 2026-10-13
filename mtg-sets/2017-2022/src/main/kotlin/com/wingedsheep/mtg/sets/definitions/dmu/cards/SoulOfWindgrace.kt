package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Soul of Windgrace
 * {1}{B}{R}{G}
 * Legendary Creature — Cat Avatar
 * 5/4
 * Whenever Soul of Windgrace enters or attacks, you may put a land card from a graveyard onto the
 * battlefield tapped under your control.
 * {G}, Discard a land card: You gain 3 life.
 * {1}{R}, Discard a land card: Draw a card.
 * {2}{B}, Discard a land card: Soul of Windgrace gains indestructible until end of turn. Tap it.
 *
 * "Enters or attacks" is two triggers sharing one effect; entering attacking isn't declaring it as
 * an attacker, so (per the ruling) only the enters half fires then. The land isn't targeted — it's
 * chosen as the trigger resolves (ruling) — so it's a Gather → Select → Move over every graveyard,
 * where choosing nothing is the "may".
 */
private val reanimateLand = Effects.Pipeline {
    val lands = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.Each, GameObjectFilter.Land))
    val chosen = chooseUpTo(
        1,
        from = lands,
        prompt = "You may put a land card from a graveyard onto the battlefield tapped under your control",
        selectedLabel = "Put onto the battlefield",
        remainderLabel = "Leave in the graveyard",
    )
    move(chosen, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You, ZonePlacement.Tapped))
}

val SoulOfWindgrace = card("Soul of Windgrace") {
    manaCost = "{1}{B}{R}{G}"
    colorIdentity = "BRG"
    typeLine = "Legendary Creature — Cat Avatar"
    power = 5
    toughness = 4
    oracleText = "Whenever Soul of Windgrace enters or attacks, you may put a land card from a graveyard " +
        "onto the battlefield tapped under your control.\n" +
        "{G}, Discard a land card: You gain 3 life.\n" +
        "{1}{R}, Discard a land card: Draw a card.\n" +
        "{2}{B}, Discard a land card: Soul of Windgrace gains indestructible until end of turn. Tap it."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = reanimateLand
        description = "When Soul of Windgrace enters, you may put a land card from a graveyard onto the " +
            "battlefield tapped under your control."
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = reanimateLand
        description = "Whenever Soul of Windgrace attacks, you may put a land card from a graveyard onto " +
            "the battlefield tapped under your control."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{G}"), Costs.Discard(Filters.Land))
        effect = Effects.GainLife(3)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{R}"), Costs.Discard(Filters.Land))
        effect = Effects.DrawCards(1)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{B}"), Costs.Discard(Filters.Land))
        effect = Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.Self) then
            Effects.Tap(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "220"
        artist = "Liiga Smilshkalne"
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e775a2e6-e701-4cb8-8c0b-718d3508f6b6.jpg?1783921275"

        ruling(
            "2022-09-09",
            "If Soul of Windgrace enters the battlefield attacking, its first ability triggers only once that combat."
        )
        ruling(
            "2022-09-09",
            "You choose the land card as the triggered ability resolves. Once the ability has begun to resolve, no player may take actions until it finishes resolving. Notably, they can't respond to the land you choose by removing it from your graveyard."
        )
        ruling(
            "2022-09-09",
            "You may activate Soul of Windgrace's last ability even if it's already tapped. It will still gain indestructible."
        )
    }
}

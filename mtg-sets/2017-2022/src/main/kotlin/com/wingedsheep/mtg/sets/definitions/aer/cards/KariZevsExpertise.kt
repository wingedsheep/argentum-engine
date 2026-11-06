package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Kari Zev's Expertise — Aether Revolt #88 (canonical / earliest real printing, 2017)
 * {1}{R}{R} · Sorcery
 *
 * Gain control of target creature or Vehicle until end of turn. Untap it. It gains haste until end of turn.
 * You may cast a spell with mana value 2 or less from your hand without paying its mana cost.
 *
 * - The threaten half is Hijack's chain ([Effects.GainControl] until end of turn, [Effects.Untap],
 *   [Effects.GrantKeyword] haste) over a [GameObjectFilter.CreatureOrVehicle] target.
 * - The Expertise rider is Sram's Expertise's free-cast hand pipeline with mana value 2 or less.
 *   If the target is illegal on resolution the whole spell fizzles, so no free spell is cast.
 */
val KariZevsExpertise = card("Kari Zev's Expertise") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Gain control of target creature or Vehicle until end of turn. Untap it. It gains haste " +
        "until end of turn.\nYou may cast a spell with mana value 2 or less from your hand without paying " +
        "its mana cost."

    spell {
        val permanent = target(TargetFilter(GameObjectFilter.CreatureOrVehicle))
        effect = Effects.GainControl(permanent, Duration.EndOfTurn) then
            Effects.Untap(permanent) then
            Effects.GrantKeyword(Keyword.HASTE, permanent) then
            Effects.Pipeline {
                val inHand = gather(CardSource.FromZone(Zone.HAND, Player.You, filter = GameObjectFilter.Nonland))
                val candidates = filter(inHand, GameObjectFilter.Any.manaValueAtMost(2))
                val chosen = chooseUpTo(1, from = candidates, selectedLabel = "Cast without paying its mana cost")
                run(Effects.CastFromCollectionWithoutPayingCost(chosen))
            }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "88"
        artist = "Jason Rainville"
        flavorText = "\"I was made for this!\""
        imageUri = "https://cards.scryfall.io/normal/front/a/b/ab5c7400-6307-4c51-88b8-9c0232110714.jpg?1783936752"
        ruling(
            "2017-04-18",
            "The mana value of a split card is determined by the combined mana cost of its two halves. If an " +
                "expertise spell allows you to cast a split card, you may cast either half or, if that split card " +
                "has fuse, both halves."
        )
        ruling(
            "2017-02-09",
            "If the Expertise spell you cast has any targets, and those targets become illegal before the spell " +
                "resolves, the spell doesn't resolve and none of its effects happen. You won't get to cast a free spell."
        )
        ruling(
            "2017-02-09",
            "Kari Zev's Expertise can target any creature or Vehicle, even one that you already control or that " +
                "is already untapped."
        )
        ruling(
            "2017-02-09",
            "If the card has {X} in its mana cost, you must choose 0 as the value of X when casting it without " +
                "paying its mana cost."
        )
        ruling("2017-02-09", "Effects that allow you to \"cast\" a card don't allow you to play a land card.")
        ruling(
            "2017-02-09",
            "While you're casting your free spell, the Expertise spell is still on the stack. It will be put into " +
                "its owner's graveyard after the free spell is cast. The free spell can't target the Expertise " +
                "card in your graveyard. It can target the Expertise spell on the stack, but the Expertise spell " +
                "will become an illegal target before the free spell resolves."
        )
        ruling(
            "2017-02-09",
            "If you cast a card \"without paying its mana cost,\" you can't choose to cast it for any alternative " +
                "costs, such as emerge costs. You can, however, pay additional costs. If the card has any " +
                "mandatory additional costs, such as that of Cathartic Reunion, you must pay those to cast the card."
        )
    }
}

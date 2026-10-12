package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Arcane Proxy — The Brothers' War #75
 * {7} · Artifact Creature — Wizard · 4/3
 *
 * Prototype {1}{U}{U} — 2/1
 * When this creature enters, if you cast it, exile target instant or sorcery card with mana value
 * less than or equal to this creature's power from your graveyard. Copy that card. You may cast the
 * copy without paying its mana cost.
 *
 * The Shiko, Paragon of the Way chain (exile → copy in exile → optional free cast) behind a
 * `Conditions.WasCast` intervening-if. The power cap rides on the target filter as
 * `manaValueAtMostDynamic(sourcePower())`, so it reads 2 when cast prototyped and 4 otherwise, and is
 * re-checked on resolution — the ruling that a lowered power makes the target illegal, so nothing is
 * exiled and no copy is made.
 */
val ArcaneProxy = card("Arcane Proxy") {
    manaCost = "{7}"
    colorIdentity = "U"
    typeLine = "Artifact Creature — Wizard"
    power = 4
    toughness = 3
    oracleText = "Prototype {1}{U}{U} — 2/1 (You may cast this spell with different mana cost, color, and size. It keeps its abilities and types.)\n" +
        "When this creature enters, if you cast it, exile target instant or sorcery card with mana value less than or " +
        "equal to this creature's power from your graveyard. Copy that card. You may cast the copy without paying its " +
        "mana cost."

    keywordAbility(KeywordAbility.prototype("{1}{U}{U}", 2, 1))

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.WasCast
        val exiledCard = target(
            TargetFilter(
                GameObjectFilter.InstantOrSorcery.ownedByYou()
                    .manaValueAtMostDynamic(DynamicAmounts.sourcePower()),
                zone = Zone.GRAVEYARD,
            ),
        )
        effect = Effects.Pipeline {
            run(Effects.Move(exiledCard, Zone.EXILE))
            val copy = copyCard(exiledCard)
            run(Effects.May(
                Effects.CastFromCollectionWithoutPayingCost(copy),
                descriptionOverride = "You may cast the copy without paying its mana cost.",
            ))
        }
        description = "When this creature enters, if you cast it, exile target instant or sorcery card with mana " +
            "value less than or equal to this creature's power from your graveyard. Copy that card. You may cast " +
            "the copy without paying its mana cost."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "75"
        artist = "Kekai Kotaki"
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d6d74d2e-7382-4fe3-ac3e-6906203ffcc7.jpg?1783920101"
        ruling(
            "2022-10-14",
            "You cast the copy while the ability is resolving and still on the stack. You can't wait to cast it " +
                "later in the turn."
        )
        ruling(
            "2022-10-14",
            "If another effect causes Arcane Proxy's power to be less than the mana value of the target card as " +
                "the ability tries to resolve, the target is illegal. The card won't be exiled, and you won't get " +
                "to cast a copy."
        )
    }
}

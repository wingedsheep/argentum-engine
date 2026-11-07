package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.effects.SelectionRestriction
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Saheeli Rai — Kaladesh #186
 * {1}{U}{R} · Legendary Planeswalker — Saheeli · Starting loyalty 3
 *
 * +1: Scry 1. Saheeli Rai deals 1 damage to each opponent.
 * −2: Create a token that's a copy of target artifact or creature you control, except it's an
 *     artifact in addition to its other types. That token gains haste. Exile it at the beginning
 *     of the next end step.
 * −7: Search your library for up to three artifact cards with different names, put them onto the
 *     battlefield, then shuffle.
 *
 * The −2's "except it's an artifact" is `addCardTypes`, a copiable value (Saheeli's Artistry's
 * shape). "That token gains haste" is *not* an except clause — haste and the delayed exile aren't
 * copiable (ruling) — so haste is a permanent granted keyword over [CollectionSlot.CreatedTokens],
 * Jaxis, the Troublemaker's shape, and the exile rides `exileAtStep`. Every token a replacement
 * effect adds is covered by both.
 *
 * The −7 is Uncage the Menagerie's search under [SelectionRestriction.OnePerCardName], moving the
 * found cards onto the battlefield together (they enter simultaneously, per the ruling) before the
 * shuffle.
 */
val SaheeliRai = card("Saheeli Rai") {
    manaCost = "{1}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Planeswalker — Saheeli"
    startingLoyalty = 3
    oracleText = "+1: Scry 1. Saheeli Rai deals 1 damage to each opponent.\n" +
        "−2: Create a token that's a copy of target artifact or creature you control, except it's an " +
        "artifact in addition to its other types. That token gains haste. Exile it at the beginning of " +
        "the next end step.\n" +
        "−7: Search your library for up to three artifact cards with different names, put them onto " +
        "the battlefield, then shuffle."

    loyaltyAbility(+1) {
        effect = Effects.Scry(1) then
            Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent))
        description = "Scry 1. Saheeli Rai deals 1 damage to each opponent."
    }

    loyaltyAbility(-2) {
        val permanent = target(
            TargetFilter((GameObjectFilter.Artifact or GameObjectFilter.Creature).youControl())
        )
        effect = Effects.CreateTokenCopyOfTarget(
            target = permanent,
            addCardTypes = setOf("ARTIFACT"),
            exileAtStep = Step.END,
        ) then Effects.ForEachInCollection(
            CollectionSlot.CreatedTokens,
            Effects.GrantKeyword(Keyword.HASTE, EffectTarget.IterationEntity, Duration.Permanent)
        )
        description = "Create a token that's a copy of target artifact or creature you control, except " +
            "it's an artifact in addition to its other types. That token gains haste. Exile it at the " +
            "beginning of the next end step."
    }

    loyaltyAbility(-7) {
        effect = Effects.Pipeline {
            val searchable = gather(
                CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.Artifact),
                search = true
            )
            val found = chooseUpTo(
                3,
                from = searchable,
                restrictions = listOf(SelectionRestriction.OnePerCardName),
                prompt = "Search for up to three artifact cards with different names to put onto the battlefield"
            )
            move(found, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
            run(Effects.ShuffleLibrary())
            // CR 701.23 — the search happened whether or not anything was found.
            run(EmitLibrarySearchedEventEffect)
        }
        description = "Search your library for up to three artifact cards with different names, put them " +
            "onto the battlefield, then shuffle."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "186"
        artist = "Willian Murai"
        imageUri = "https://cards.scryfall.io/normal/front/9/4/94b38464-39cd-4ee6-b9bf-a0bc1e128d9a.jpg?1783937167"

        ruling("2016-09-20", "The token copies exactly what was printed on the original permanent and nothing else (unless that permanent is copying something else or is a token; see below). It doesn't copy whether that permanent is tapped or untapped, whether it has any counters on it or Auras and Equipment attached to it, or any non-copy effects that have changed its power, toughness, types, color, or so on.")
        ruling("2016-09-20", "The token is an artifact in addition to its other types. This is a copiable value of the token that other effects may copy. Haste and the ability's delayed triggered ability aren't copiable.")
        ruling("2016-09-20", "If the copied permanent has {X} in its mana cost, X is considered to be 0.")
        ruling("2016-09-20", "If the copied permanent is copying something else (for example, if the copied permanent is an Altered Ego), then the token enters the battlefield as whatever that permanent copied.")
        ruling("2016-09-20", "If the copied permanent is a token, the token created with Saheeli copies the original characteristics of that token as stated by the effect that created that token.")
        ruling("2016-09-20", "Any enters-the-battlefield abilities of the copied permanent will trigger when the token enters the battlefield. Any \"as [this permanent] enters the battlefield\" or \"[this permanent] enters the battlefield with\" abilities of the chosen permanent will also work.")
        ruling("2016-09-20", "As Saheeli's last ability resolves, all three artifacts enter the battlefield at the same time. Triggered abilities of any of those artifacts will see all three entering the battlefield. Replacement effects, such as that of Sculpting Steel, won't see the others on the battlefield as they are being applied.")
    }
}

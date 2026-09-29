package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyCounterPlacement
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ozolith, the Shattered Spire
 * {1}{G}
 * Legendary Artifact
 * If one or more +1/+1 counters would be put on an artifact or creature you control, that many
 * plus one +1/+1 counters are put on it instead.
 * {1}{G}, {T}: Put a +1/+1 counter on target artifact or creature you control. Activate only as a
 * sorcery.
 * Cycling {2}
 *
 * The first ability is Hardened Scales' [ModifyCounterPlacement] with the recipient widened to
 * "an artifact or creature you control". Being a counter-placement replacement, it covers
 * permanents entering with +1/+1 counters and stacks with additional copies (per the rulings).
 */
private val ArtifactOrCreatureYouControl =
    (GameObjectFilter.Artifact or GameObjectFilter.Creature).youControl()

val OzolithTheShatteredSpire = card("Ozolith, the Shattered Spire") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Artifact"
    oracleText = "If one or more +1/+1 counters would be put on an artifact or creature you control, " +
        "that many plus one +1/+1 counters are put on it instead.\n" +
        "{1}{G}, {T}: Put a +1/+1 counter on target artifact or creature you control. " +
        "Activate only as a sorcery.\n" +
        "Cycling {2} ({2}, Discard this card: Draw a card.)"

    replacementEffect(
        ModifyCounterPlacement(
            modifier = 1,
            appliesTo = EventPattern.CounterPlacementEvent(
                counterType = CounterType.PLUS_ONE_PLUS_ONE,
                recipient = Recipient.Object(ArtifactOrCreatureYouControl),
            ),
        )
    )

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{G}"), Costs.Tap)
        val t = target(TargetFilter.CreatureOrArtifact.youControl())
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, t)
        timing = TimingRule.SorcerySpeed
        description = "Put a +1/+1 counter on target artifact or creature you control. Activate only as a sorcery."
    }

    keywordAbility(KeywordAbility.cycling("{2}"))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "198"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c9eee658-29e8-4ab5-8a0e-31f2f28a9a92.jpg?1783916964"
        ruling(
            "2023-04-14",
            "If another artifact or creature you control would enter the battlefield with a number " +
                "of +1/+1 counters on it, it enters with that many plus one instead."
        )
        ruling(
            "2023-04-14",
            "However, if Ozolith, the Shattered Spire somehow enters the battlefield with +1/+1 " +
                "counters on it, its first ability won't apply to itself."
        )
        ruling(
            "2023-04-14",
            "If two or more effects attempt to modify how many counters would be put onto a " +
                "permanent you control, you choose the order to apply those effects, no matter who " +
                "controls the sources of those effects."
        )
        ruling(
            "2023-04-14",
            "If you somehow control two copies of Ozolith, the Shattered Spire, the number of " +
                "+1/+1 counters put on an artifact or creature you control is two plus the original " +
                "number. Three Ozoliths add three, and so on."
        )
    }
}

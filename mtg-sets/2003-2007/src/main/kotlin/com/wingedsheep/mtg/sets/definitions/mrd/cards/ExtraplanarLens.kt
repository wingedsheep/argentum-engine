package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalManaOnSourceTap
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Extraplanar Lens — Mirrodin #169 (canonical printing)
 * {3} · Artifact · Rare
 *
 * Imprint — When this artifact enters, you may exile target land you control.
 * Whenever a land with the same name as the exiled card is tapped for mana, its controller adds
 * one mana of any type that land produced.
 *
 * Modelling notes:
 * - The two halves are a *linked* pair (CR 607): the static reads only what this artifact's own ETB
 *   trigger exiled, via `Effects.ExileLinkedToSource` writing the pile and
 *   [EntityReference.LinkedExiledCard] naming its card. A "cards in exile" scan would pick up every
 *   other exiled land in the game.
 * - The doubling half is *not* a new primitive. "Its controller adds one mana of any type that land
 *   produced" is Lavaleaper's printed wording, and [AdditionalManaOnSourceTap] with `color = null`
 *   is exactly that: mirror whatever the tapped land produced. The only thing this card needed was
 *   a way to *say* which lands qualify — `sharingNameWith`, the name sibling of the
 *   `sharesManaValueWith` that Thought Prison already reads off the same pile.
 * - **No `youControl()` on the filter.** The printed text says "a land", not "a land you control",
 *   and "its controller adds" — so an opponent who also runs Snow-Covered Forests gets the bonus
 *   from your Lens too. Exiling a basic type your opponents don't play is how the card is
 *   *steered*, not something it enforces; encoding it as `youControl()` would silently change a
 *   symmetric card into a one-sided one.
 * - Declining the imprint (or exiling a land that later leaves exile) leaves the filter with no
 *   name to match, so the static goes inert on its own: `SharesNameWith` matches nothing when its
 *   reference resolves to nothing, and `LinkedExileLookup` only counts a linked card while it is
 *   still in exile.
 * - The trigger targets, so the "you may" lowers to a resolution-time gate and the trigger is
 *   simply removed for want of a legal target if you control no land.
 */
val ExtraplanarLens = card("Extraplanar Lens") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Imprint — When this artifact enters, you may exile target land you control.\n" +
        "Whenever a land with the same name as the exiled card is tapped for mana, its controller " +
        "adds one mana of any type that land produced."

    // "Imprint — When this artifact enters, you may exile target land you control."
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        optional = true
        val land = target(
            "target land you control",
            TargetPermanent(filter = TargetFilter.Land.youControl())
        )
        effect = Effects.ExileLinkedToSource(land)
        description = "Imprint — When this artifact enters, you may exile target land you control."
    }

    // "Whenever a land with the same name as the exiled card is tapped for mana, its controller
    // adds one mana of any type that land produced." (Triggered mana ability — off-stack, CR 605.1.)
    staticAbility {
        ability = AdditionalManaOnSourceTap(
            sourceFilter = GameObjectFilter.Land
                .anyController()
                .sharingNameWith(EntityReference.LinkedExiledCard()),
            color = null
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "169"
        artist = "Lars Grant-West"
        imageUri = "https://cards.scryfall.io/normal/front/6/2/622a6523-3b12-4657-a656-00a57a3ae59c.jpg"
    }
}

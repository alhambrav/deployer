/*
 * Copyright (C) 2007-2017 Crafter Software Corporation.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.craftercms.deployer.utils.handlebars;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;

import java.io.IOException;

import org.craftercms.deployer.api.exceptions.MissingRequiredParameterException;

/**
 * Created by alfonsovasquez on 1/30/17.
 */
public class MissingValueHelper implements Helper<Object> {

    public static final MissingValueHelper INSTANCE = new MissingValueHelper();

    @Override
    public Object apply(Object context, Options options) throws IOException {
        throw new IOException(new MissingRequiredParameterException(options.helperName));
    }

}

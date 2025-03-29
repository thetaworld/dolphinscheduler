/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dolphinscheduler.e2e.pages.project.workflow.task;

import org.apache.dolphinscheduler.e2e.core.WebDriverWaitFactory;
import org.apache.dolphinscheduler.e2e.pages.project.workflow.WorkflowForm;

import java.util.List;

import lombok.Getter;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.FindBys;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;

@Getter
public abstract class TaskNodeForm {

    @FindBys({
            @FindBy(className = "input-node-name"),
            @FindBy(tagName = "input")
    })
    private WebElement inputNodeName;

    @FindBy(className = "btn-submit")
    private WebElement buttonSubmit;

    @FindBys({
            @FindBy(className = "input-param-key"),
            @FindBy(tagName = "input"),
    })
    private List<WebElement> inputParamKey;

    @FindBys({
            @FindBy(className = "input-param-value"),
            @FindBy(tagName = "input"),
    })
    private List<WebElement> inputParamValue;

    @FindBys({
            @FindBy(className = "pre-tasks-model"),
            @FindBy(className = "n-base-selection"),
    })
    private WebElement selectPreTasks;

    @FindBys({
            @FindBy(className = "env-select"),
            @FindBy(className = "n-base-selection"),
    })
    private WebElement selectEnv;

    @FindBys({
            @FindBy(className = "btn-custom-parameters"),
            @FindBy(tagName = "button"),
    })
    private WebElement buttonCustomParameters;

    @FindBy(className = "btn-create-custom-parameter")
    private WebElement buttonCreateCustomParameters;

    @FindBys({
            @FindBy(className = "resource-select"),
            @FindBy(className = "n-base-selection"),
    })
    private WebElement selectResource;

    private final WorkflowForm parent;

    TaskNodeForm(WorkflowForm parent) {
        this.parent = parent;

        final WebDriver driver = parent.driver();

        PageFactory.initElements(driver, this);
    }

    public TaskNodeForm name(String name) {
        inputNodeName().sendKeys(name);

        return this;
    }

    public TaskNodeForm addParam(String key, String value) {
        assert inputParamKey().size() == inputParamValue().size();

        final int len = inputParamKey().size();

        final WebDriver driver = parent().driver();

        if (len == 0) {
            buttonCustomParameters().click();

            inputParamKey().get(0).sendKeys(key);
            inputParamValue().get(0).sendKeys(value);
        } else {
            buttonCreateCustomParameters().click();

            inputParamKey().get(len).sendKeys(key);
            inputParamValue().get(len).sendKeys(value);
        }

        return this;
    }

    public TaskNodeForm selectEnv(String envName) {
        ((JavascriptExecutor) parent().driver()).executeScript("arguments[0].click();", selectEnv);

        final By optionsLocator = By.className("n-base-selection-input__content");

        WebDriverWaitFactory.createWebDriverWait(parent().driver())
                .until(ExpectedConditions.visibilityOfElementLocated(optionsLocator));

        List<WebElement> webElements = parent.driver().findElements(optionsLocator);

        webElements.stream()
                .filter(it -> it.getText().contains(envName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No such envName: " + envName))
                .click();

        return this;
    }

    public TaskNodeForm preTask(String preTaskName) {
        ((JavascriptExecutor) parent().driver()).executeScript("arguments[0].click();", selectPreTasks);

        final By optionsLocator = By.className("option-pre-tasks");

        WebDriverWaitFactory.createWebDriverWait(parent.driver())
                .until(ExpectedConditions.visibilityOfElementLocated(optionsLocator));

        List<WebElement> webElements = parent.driver().findElements(optionsLocator);
        webElements.stream()
                .filter(it -> it.getText().contains(preTaskName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No such task: " + preTaskName))
                .click();

        inputNodeName().click();

        return this;
    }

    public TaskNodeForm selectResource(String resourceName) {
        ((JavascriptExecutor) parent().driver()).executeScript("arguments[0].click();", selectResource);

        final By optionsLocator = By.className("n-tree-node-content__text");

        WebDriverWaitFactory.createWebDriverWait(parent().driver())
                .until(ExpectedConditions.visibilityOfElementLocated(optionsLocator));

        parent().driver()
                .findElements(optionsLocator)
                .stream()
                .filter(it -> it.getText().startsWith(resourceName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No such resource: " + resourceName))
                .click();

        parent.driver().switchTo().activeElement().sendKeys(Keys.ESCAPE);
        return this;
    }

    public WorkflowForm submit() {
        buttonSubmit.click();

        return parent();
    }
}
